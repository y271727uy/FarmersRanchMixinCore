package com.y271727uy.FRMC.coremod.mcreator;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * MCreator 生成代码的只读扫描器（阶段 1：识别 + 报告，不改写）。
 *
 * 统计的模式：
 * <ul>
 *   <li>ANON_NBT_GETVALUE：new Object(){ getValue(LevelAccessor, BlockPos, String) } 匿名类读 NBT 的调用点</li>
 *   <li>PLAYER_VARS_ORELSE：getCapability(...).orElse(new PlayerVariables()) 每次新建兜底对象</li>
 *   <li>PLAYER_VARS_SYNC：syncPlayerVariables 调用点</li>
 *   <li>SEND_BLOCK_UPDATED：sendBlockUpdated 调用点</li>
 *   <li>GET_BLOCK_STATE / GET_BLOCK_ENTITY：方块查询调用点（同一方法内多次出现时另计 REPEATED_*）</li>
 *   <li>EVENT_SUBSCRIBER：@SubscribeEvent 方法，按事件类型分类</li>
 *   <li>PLAYER_VARS_ORELSEGET / PLAYER_VARS_MARK_DIRTY：新版 MCreator 的写法（懒创建 / 批量同步），
 *       只作为生成器版本指纹，不需要优化</li>
 *   <li>MIXIN_MERGED：已被 Mixin 合并过的类（用于确认本插件在 Mixin 之后运行，以及跳过手写 @Overwrite 的类）</li>
 * </ul>
 *
 * 生产环境方法名是 SRG，开发环境是 Mojang 名，两者都要匹配。
 */
final class McreatorScanner {
    private McreatorScanner() {
    }

    enum Pattern {
        ANON_NBT_GETVALUE,
        PLAYER_VARS_ORELSE,
        PLAYER_VARS_SYNC,
        PLAYER_VARS_ORELSEGET,
        PLAYER_VARS_MARK_DIRTY,
        SEND_BLOCK_UPDATED,
        GET_BLOCK_STATE,
        REPEATED_GET_BLOCK_STATE,
        GET_BLOCK_ENTITY,
        REPEATED_GET_BLOCK_ENTITY,
        EVENT_SUBSCRIBER
    }

    private static final String MIXIN_MERGED_DESC = "Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;";
    private static final String SUBSCRIBE_EVENT_DESC = "Lnet/minecraftforge/eventbus/api/SubscribeEvent;";
    private static final String LAZY_OPTIONAL = "net/minecraftforge/common/util/LazyOptional";
    private static final String ANON_GETVALUE_DESC_PREFIX =
        "(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Ljava/lang/String;)";
    /** orElse 之前往回找 NEW 指令的最大实际指令数。new X() 之后一般紧跟 dup + invokespecial。 */
    private static final int ORELSE_LOOKBACK = 4;

    /** modid -> 统计 */
    private static final Map<String, ModStats> STATS = new ConcurrentHashMap<>();
    /** 带命中的类 -> 命中计数，用于报告中的热点列表 */
    private static final Map<String, int[]> CLASS_HITS = new ConcurrentHashMap<>();
    private static final Map<String, LongAdder> EVENT_TYPES = new ConcurrentHashMap<>();

    static final class ModStats {
        final LongAdder classes = new LongAdder();
        final LongAdder procedureClasses = new LongAdder();
        final LongAdder mixinMergedClasses = new LongAdder();
        final LongAdder[] patterns = new LongAdder[Pattern.values().length];

        ModStats() {
            for (int i = 0; i < patterns.length; i++) {
                patterns[i] = new LongAdder();
            }
        }
    }

    /** 预加载本类及内部类、枚举，并跑一次空扫描。 */
    static void warmup() {
        ClassNode dummy = new ClassNode();
        dummy.name = "net/mcreator/frmc_warmup/procedures/WarmupProcedure";
        dummy.methods.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "execute", "()V", null, null));
        scan(dummy);
        STATS.clear();
        CLASS_HITS.clear();
        EVENT_TYPES.clear();
        Pattern.values();
        new ModStats();
        new StringBuilder().append(1L);
    }

    static void scan(ClassNode node) {
        String name = node.name; // net/mcreator/<modid>/...
        int modStart = "net/mcreator/".length();
        int modEnd = name.indexOf('/', modStart);
        if (modEnd < 0) {
            return;
        }
        String modId = name.substring(modStart, modEnd);
        ModStats stats = STATS.computeIfAbsent(modId, k -> new ModStats());
        stats.classes.increment();

        boolean procedure = name.contains("/procedures/");
        if (procedure && name.indexOf('$') < 0) {
            stats.procedureClasses.increment();
        }

        int[] hits = new int[Pattern.values().length];
        boolean mixinMerged = false;

        for (MethodNode method : node.methods) {
            if (!mixinMerged && (hasAnnotation(method.visibleAnnotations, MIXIN_MERGED_DESC)
                || hasAnnotation(method.invisibleAnnotations, MIXIN_MERGED_DESC))) {
                mixinMerged = true;
            }
            if (hasAnnotation(method.visibleAnnotations, SUBSCRIBE_EVENT_DESC)) {
                hits[Pattern.EVENT_SUBSCRIBER.ordinal()]++;
                Type[] args = Type.getArgumentTypes(method.desc);
                if (args.length > 0) {
                    EVENT_TYPES.computeIfAbsent(args[0].getClassName(), k -> new LongAdder()).increment();
                }
            }
            scanMethod(method, hits);
        }

        if (mixinMerged) {
            stats.mixinMergedClasses.increment();
        }
        int total = 0;
        for (int i = 0; i < hits.length; i++) {
            if (hits[i] != 0) {
                stats.patterns[i].add(hits[i]);
                total += hits[i];
            }
        }
        if (total > 0) {
            CLASS_HITS.put(name.replace('/', '.') + (mixinMerged ? " [mixin]" : ""), hits);
        }
    }

    private static void scanMethod(MethodNode method, int[] hits) {
        if (method.instructions == null || method.instructions.size() == 0) {
            return;
        }
        int blockStates = 0;
        int blockEntities = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode call)) {
                continue;
            }
            String n = call.name;
            switch (n) {
                case "getBlockState", "m_8055_" -> {
                    if (call.desc.startsWith("(Lnet/minecraft/core/BlockPos;)")) {
                        blockStates++;
                    }
                }
                case "getBlockEntity", "m_7702_" -> {
                    if (call.desc.startsWith("(Lnet/minecraft/core/BlockPos;)")) {
                        blockEntities++;
                    }
                }
                case "sendBlockUpdated", "m_7260_" -> hits[Pattern.SEND_BLOCK_UPDATED.ordinal()]++;
                case "syncPlayerVariables" -> hits[Pattern.PLAYER_VARS_SYNC.ordinal()]++;
                case "markSyncDirty" -> {
                    if (call.owner.endsWith("$PlayerVariables")) {
                        hits[Pattern.PLAYER_VARS_MARK_DIRTY.ordinal()]++;
                    }
                }
                case "orElseGet" -> {
                    if (LAZY_OPTIONAL.equals(call.owner) && checkcastPlayerVariablesAfter(call)) {
                        hits[Pattern.PLAYER_VARS_ORELSEGET.ordinal()]++;
                    }
                }
                case "getValue" -> {
                    if (call.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && call.owner.indexOf('$') >= 0
                        && call.desc.startsWith(ANON_GETVALUE_DESC_PREFIX)) {
                        hits[Pattern.ANON_NBT_GETVALUE.ordinal()]++;
                    }
                }
                case "orElse" -> {
                    if (LAZY_OPTIONAL.equals(call.owner) && newPlayerVariablesBefore(call)) {
                        hits[Pattern.PLAYER_VARS_ORELSE.ordinal()]++;
                    }
                }
                default -> {
                }
            }
        }
        hits[Pattern.GET_BLOCK_STATE.ordinal()] += blockStates;
        hits[Pattern.GET_BLOCK_ENTITY.ordinal()] += blockEntities;
        if (blockStates > 1) {
            hits[Pattern.REPEATED_GET_BLOCK_STATE.ordinal()] += blockStates - 1;
        }
        if (blockEntities > 1) {
            hits[Pattern.REPEATED_GET_BLOCK_ENTITY.ordinal()] += blockEntities - 1;
        }
    }

    private static boolean newPlayerVariablesBefore(AbstractInsnNode call) {
        int seen = 0;
        for (AbstractInsnNode p = call.getPrevious(); p != null && seen < ORELSE_LOOKBACK; p = p.getPrevious()) {
            if (p.getOpcode() < 0) {
                continue; // label / line number / frame
            }
            seen++;
            if (p.getOpcode() == Opcodes.NEW && ((TypeInsnNode) p).desc.endsWith("$PlayerVariables")) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkcastPlayerVariablesAfter(AbstractInsnNode call) {
        for (AbstractInsnNode n = call.getNext(); n != null; n = n.getNext()) {
            if (n.getOpcode() < 0) {
                continue;
            }
            return n.getOpcode() == Opcodes.CHECKCAST && ((TypeInsnNode) n).desc.endsWith("$PlayerVariables");
        }
        return false;
    }

    /** 粗略判断 MCreator 生成器代际：旧版每次 new 兜底对象并立即同步，新版懒创建并标脏批量同步。 */
    private static String generation(ModStats s) {
        long legacy = s.patterns[Pattern.PLAYER_VARS_ORELSE.ordinal()].sum()
            + s.patterns[Pattern.PLAYER_VARS_SYNC.ordinal()].sum()
            + s.patterns[Pattern.ANON_NBT_GETVALUE.ordinal()].sum();
        long modern = s.patterns[Pattern.PLAYER_VARS_ORELSEGET.ordinal()].sum()
            + s.patterns[Pattern.PLAYER_VARS_MARK_DIRTY.ordinal()].sum();
        if (legacy > 0 && modern > 0) {
            return "mixed";
        }
        if (legacy > 0) {
            return "legacy";
        }
        return modern > 0 ? "modern" : "unknown";
    }

    private static boolean hasAnnotation(List<AnnotationNode> annotations, String desc) {
        if (annotations == null) {
            return false;
        }
        for (AnnotationNode a : annotations) {
            if (desc.equals(a.desc)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- 报告

    static void registerShutdownReport() {
        Thread hook = new Thread(McreatorScanner::writeReport, "FRMC-MCreatorScanReport");
        hook.setDaemon(false);
        Runtime.getRuntime().addShutdownHook(hook);
    }

    static String buildReport() {
        StringBuilder sb = new StringBuilder(4096);
        Pattern[] patterns = Pattern.values();
        sb.append("FRMC MCreator scan report (report-only, no bytecode rewritten)\n");
        sb.append("Note: classes are counted only when actually loaded; procedures never triggered in this session are not included.\n\n");

        sb.append("== Per mod ==\n");
        Map<String, ModStats> sorted = new TreeMap<>(STATS);
        for (Map.Entry<String, ModStats> e : sorted.entrySet()) {
            ModStats s = e.getValue();
            sb.append(e.getKey())
                .append(": classes=").append(s.classes.sum())
                .append(" procedures=").append(s.procedureClasses.sum())
                .append(" mixinMerged=").append(s.mixinMergedClasses.sum())
                .append(" generation=").append(generation(s))
                .append('\n');
            for (Pattern p : patterns) {
                long v = s.patterns[p.ordinal()].sum();
                if (v != 0) {
                    sb.append("    ").append(p).append(" = ").append(v).append('\n');
                }
            }
        }

        sb.append("\n== Totals ==\n");
        for (Pattern p : patterns) {
            long v = 0;
            for (ModStats s : STATS.values()) {
                v += s.patterns[p.ordinal()].sum();
            }
            sb.append(p).append(" = ").append(v).append('\n');
        }

        sb.append("\n== Event subscriber types ==\n");
        List<Map.Entry<String, LongAdder>> events = new ArrayList<>(EVENT_TYPES.entrySet());
        events.sort(Comparator.comparingLong((Map.Entry<String, LongAdder> en) -> en.getValue().sum()).reversed());
        for (Map.Entry<String, LongAdder> en : events) {
            sb.append(en.getValue().sum()).append("  ").append(en.getKey()).append('\n');
        }

        sb.append("\n== Classes with hits (top 100 by optimisable hits) ==\n");
        List<Map.Entry<String, int[]>> classes = new ArrayList<>(CLASS_HITS.entrySet());
        classes.sort(Comparator.comparingInt((Map.Entry<String, int[]> en) -> optimisable(en.getValue())).reversed());
        int limit = Math.min(100, classes.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, int[]> en = classes.get(i);
            sb.append(en.getKey()).append(" :");
            int[] h = en.getValue();
            for (Pattern p : patterns) {
                if (h[p.ordinal()] != 0) {
                    sb.append(' ').append(p).append('=').append(h[p.ordinal()]);
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** 排序用：只计可直接优化的模式，原始查询次数不计入。 */
    private static int optimisable(int[] h) {
        return h[Pattern.ANON_NBT_GETVALUE.ordinal()]
            + h[Pattern.PLAYER_VARS_ORELSE.ordinal()]
            + h[Pattern.PLAYER_VARS_SYNC.ordinal()]
            + h[Pattern.SEND_BLOCK_UPDATED.ordinal()]
            + h[Pattern.REPEATED_GET_BLOCK_STATE.ordinal()]
            + h[Pattern.REPEATED_GET_BLOCK_ENTITY.ordinal()];
    }

    static void writeReport() {
        try {
            Path dir = gameDir().resolve("logs");
            Files.createDirectories(dir);
            Path file = dir.resolve("frmc-mcreator-scan.txt");
            Files.writeString(file, buildReport(), StandardCharsets.UTF_8);
            System.out.println("[FRMC] MCreator scan report written to " + file.toAbsolutePath());
        } catch (IOException | RuntimeException e) {
            System.err.println("[FRMC] Failed to write MCreator scan report: " + e);
        }
    }

    private static Path gameDir() {
        try {
            return Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.GAMEDIR.get()).orElseGet(() -> Paths.get("."));
        } catch (Throwable t) {
            return Paths.get(".");
        }
    }
}
