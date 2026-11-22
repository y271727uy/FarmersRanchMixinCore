package com.y271727uy.FRMC.capability.villager.diagnostics;

import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.capability.villager.diagnostics.VillagerProfileSession.PathOutcome;
import com.y271727uy.FRMC.capability.villager.diagnostics.VillagerProfileSession.Phase;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import com.y271727uy.FRMC.mixin.minecraft.villager.diagnostics.GateBehaviorAccessor;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.pathfinder.Node;

/**
 * Read-only, command-driven profiler for vanilla villager AI. While no session runs every hook is a single
 * volatile read. It never changes what the villager does; it only times Brain phases, sensor scans and
 * vanilla path searches of entities whose type is exactly {@link EntityType#VILLAGER}.
 */
public final class VillagerProfiler {
    public static final int DEFAULT_SECONDS = 30;
    public static final int MAX_SECONDS = 300;
    public static final int REPORT_ROWS = 8;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_CALLER_FRAMES = 48;
    private static final StackWalker WALKER = StackWalker.getInstance();
    private static final String DECLARATIVE_PACKAGE = "net.minecraft.world.entity.ai.behavior.declarative.";
    private static final int MAX_BEHAVIOR_NAMES = 32768;
    private static final int MAX_COMPOSITE_NAME = 96;
    /** Behavior instances are per brain; names are cached for the session and dropped with it. */
    private static final Map<Object, String> BEHAVIOR_NAMES = new IdentityHashMap<>();

    private static final String OUTSIDE_BEHAVIOR = "<outside behavior>";

    private static volatile boolean active;
    private static LivingEntity currentVillager;
    private static Object currentBehavior;
    private static volatile Thread serverThread;
    private static VillagerProfileSession session;
    private static VillagerProfileReport lastReport;
    private static CommandSourceStack requester;

    private VillagerProfiler() {
    }

    public static boolean start(int seconds, CommandSourceStack source) {
        if (active) {
            return false;
        }
        int clamped = Math.max(1, Math.min(seconds, MAX_SECONDS));
        session = new VillagerProfileSession(System.nanoTime(), clamped * 20);
        requester = source;
        serverThread = Thread.currentThread();
        active = true;
        return true;
    }

    /** Stops the running session early and keeps its partial report. */
    public static boolean stop() {
        if (!active) {
            return false;
        }
        finish();
        return true;
    }

    public static boolean isActive() {
        return active;
    }

    public static VillagerProfileReport lastReport() {
        return lastReport;
    }

    /** Hot-path gate shared by every mixin hook. */
    public static boolean tracks(LivingEntity entity) {
        return active && entity.getType() == EntityType.VILLAGER && Thread.currentThread() == serverThread;
    }

    public static void recordBrainTick(LivingEntity villager, long nanos) {
        VillagerProfileSession current = session;
        if (current != null) {
            int id = villager.getId();
            current.recordBrainTick(id, current.knowsVillager(id) ? null : label(villager), nanos);
        }
    }

    public static void recordPhase(Phase phase, long nanos) {
        VillagerProfileSession current = session;
        if (current != null) {
            current.recordPhase(phase, nanos);
        }
    }

    public static void recordSensor(Object sensor, long nanos) {
        VillagerProfileSession current = session;
        if (current != null) {
            current.recordSensor(sensor.getClass().getSimpleName(), nanos);
        }
    }

    /** Path searches may come from other threads in modded setups; those are counted but never aggregated. */
    public static boolean tracksPath(LivingEntity entity) {
        if (!active || entity.getType() != EntityType.VILLAGER) {
            return false;
        }
        if (Thread.currentThread() != serverThread) {
            VillagerProfileSession current = session;
            if (current != null) {
                current.recordOffThreadPath();
            }
            return false;
        }
        return true;
    }

    public static void recordPath(LivingEntity villager, Set<BlockPos> targets, Path path, long nanos) {
        VillagerProfileSession current = session;
        if (current == null) {
            return;
        }
        PathOutcome outcome = path == null ? PathOutcome.NONE : path.canReach() ? PathOutcome.REACHED : PathOutcome.PARTIAL;
        int id = villager.getId();
        String label = current.knowsVillager(id) ? null : label(villager);
        current.recordPath(id, label, caller(VillagerProfiler::isPathInfrastructure), targets.size(),
                path == null ? 0 : path.getNodeCount(), outcome, nanos);
        if (outcome != PathOutcome.REACHED) {
            current.recordUnreachedSample(id, null, unreachedSample(villager, targets, path));
        }
    }

    /** Counts one application of an FRMC villager optimization, so before/after runs show how often it fired. */
    public static void recordOptimization(LivingEntity villager, String optimization) {
        if (!tracks(villager)) {
            return;
        }
        VillagerProfileSession current = session;
        if (current != null) {
            current.recordOptimization(optimization);
        }
    }

    /** Marks whose brain is ticking so memory writes can be attributed to that villager. */
    public static LivingEntity enterBrain(LivingEntity villager) {
        LivingEntity previous = currentVillager;
        currentVillager = villager;
        return previous;
    }

    public static void exitBrain(LivingEntity previous) {
        currentVillager = previous;
    }

    public static void recordBehaviorStart(Object behavior, long nanos) {
        VillagerProfileSession current = session;
        if (current != null) {
            current.recordBehaviorStart(behaviorName(behavior), nanos);
        }
    }

    public static void recordBehaviorTick(Object behavior, long nanos) {
        VillagerProfileSession current = session;
        if (current != null) {
            current.recordBehaviorTick(behaviorName(behavior), nanos);
        }
    }

    /** Marks which top-level behavior the Brain is running, so memory writes are attributed without a stack walk. */
    public static Object enterBehavior(Object behavior) {
        Object previous = currentBehavior;
        currentBehavior = behavior;
        return previous;
    }

    public static void exitBehavior(Object previous) {
        currentBehavior = previous;
    }

    /**
     * Called for every brain memory write; only WALK_TARGET values inside a tracked villager's brain tick count.
     * The running top-level behavior names the writer. Only composites (RunOne and other GateBehaviors) pay for a
     * stack walk, to find which child wrote; a plain stack walk per write cost tens of microseconds and inflated
     * TICK_BEHAVIORS badly when TradeWithVillager rewrites its walk target every tick.
     */
    public static void recordMemoryWrite(MemoryModuleType<?> type, Optional<?> value) {
        LivingEntity villager = currentVillager;
        VillagerProfileSession current = session;
        if (villager == null || current == null || type != MemoryModuleType.WALK_TARGET || value.isEmpty()) {
            return;
        }
        Object behavior = currentBehavior;
        String setter;
        if (behavior == null) {
            setter = OUTSIDE_BEHAVIOR;
        } else if (behavior instanceof GateBehavior<?>) {
            setter = compositeWriter(behavior);
        } else {
            setter = behaviorName(behavior);
        }
        int id = villager.getId();
        current.recordWalkTargetSet(id, current.knowsVillager(id) ? null : label(villager), setter);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && active && session != null && session.tick()) {
            finish();
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        active = false;
        session = null;
        requester = null;
        serverThread = null;
        currentVillager = null;
        currentBehavior = null;
        BEHAVIOR_NAMES.clear();
    }

    private static void finish() {
        active = false;
        VillagerProfileSession finished = session;
        session = null;
        serverThread = null;
        currentVillager = null;
        currentBehavior = null;
        BEHAVIOR_NAMES.clear();
        if (finished == null) {
            return;
        }
        lastReport = finished.finish(System.nanoTime(), REPORT_ROWS);
        for (VillagerProfileLines.Line line : VillagerProfileLines.of(lastReport)) {
            LOGGER.info(line.format());
        }
        CommandSourceStack source = requester;
        requester = null;
        if (source != null) {
            try {
                VillagerProfilerCommands.send(source, lastReport);
            } catch (RuntimeException ignored) {
                // The requester may have disconnected; the report is still in the log and /report.
            }
        }
    }

    private static String label(LivingEntity entity) {
        String profession = entity instanceof Villager villager
                ? villager.getVillagerData().getProfession().toString() : "?";
        if (entity.isBaby()) {
            profession = "baby_" + profession;
        }
        return profession + " @ " + entity.getBlockX() + "," + entity.getBlockY() + "," + entity.getBlockZ();
    }

    /**
     * The first stack frame that is not infrastructure names who asked, for example AcquirePoi,
     * MoveToTargetSink or Mob (navigation recompute while following a path).
     */
    private static String caller(Predicate<String> infrastructure) {
        Optional<String> found = WALKER.walk(frames -> frames
                .limit(MAX_CALLER_FRAMES)
                .map(StackWalker.StackFrame::getClassName)
                .filter(name -> !infrastructure.test(name))
                .findFirst());
        return found.map(VillagerProfiler::simpleName).orElse(VillagerProfileSession.OTHER_BUCKET);
    }

    private static boolean isCommonInfrastructure(String className) {
        return className.startsWith("java.") || className.startsWith("jdk.")
                || className.startsWith("com.llamalad7.mixinextras.")
                || className.startsWith("com.mojang.datafixers.")
                || className.startsWith("com.y271727uy.FRMC.capability.villager.diagnostics.");
    }

    private static boolean isPathInfrastructure(String className) {
        return isCommonInfrastructure(className)
                || className.startsWith("net.minecraft.world.entity.ai.navigation.")
                || className.startsWith("net.minecraft.world.level.pathfinder.");
    }

    /** Skips Brain, the declarative behavior plumbing and BehaviorUtils so the real behavior is named. */
    private static boolean isMemoryInfrastructure(String className) {
        return isCommonInfrastructure(className)
                || className.equals("net.minecraft.world.entity.ai.Brain")
                || className.startsWith("net.minecraft.world.entity.ai.Brain$")
                || className.startsWith("net.minecraft.world.entity.ai.behavior.declarative.")
                || className.equals("net.minecraft.world.entity.ai.behavior.BehaviorUtils")
                || className.equals("net.minecraft.world.entity.ai.behavior.OneShot");
    }

    /**
     * Inside a composite, the writer is its running child. A child only becomes RUNNING after its own start
     * succeeds, so writes made while a child is starting fall back to a stack walk; those are rare compared
     * with running children such as TradeWithVillager, which rewrites its walk target every tick.
     */
    private static String compositeWriter(Object composite) {
        String parent = behaviorName(composite);
        for (Object child : ((GateBehaviorAccessor) composite).frmc$children()) {
            if (child instanceof BehaviorControl<?> control && control.getStatus() == Behavior.Status.RUNNING) {
                return parent + ">" + behaviorName(child);
            }
        }
        return parent + ">" + caller(VillagerProfiler::isMemoryInfrastructure);
    }

    private static String behaviorName(Object behavior) {
        String name = BEHAVIOR_NAMES.get(behavior);
        if (name == null) {
            name = describeBehavior(behavior);
            if (BEHAVIOR_NAMES.size() < MAX_BEHAVIOR_NAMES) {
                BEHAVIOR_NAMES.put(behavior, name);
            }
        }
        return name;
    }

    /**
     * Named behavior classes use their class name. Declarative OneShot behaviors are anonymous, so their
     * debug string is searched for lambda owners (for example "AcquirePoi$$Lambda") to recover the factory.
     */
    static String describeBehavior(Object behavior) {
        Class<?> type = behavior.getClass();
        String simple = type.getSimpleName();
        if (behavior instanceof GateBehavior<?>) {
            StringBuilder name = new StringBuilder(simple.isEmpty() ? "GateBehavior" : simple).append('[');
            boolean first = true;
            for (Object child : ((GateBehaviorAccessor) behavior).frmc$children()) {
                if (!first) {
                    name.append(',');
                }
                first = false;
                name.append(describeBehavior(child));
                if (name.length() > MAX_COMPOSITE_NAME) {
                    name.setLength(MAX_COMPOSITE_NAME);
                    name.append("...");
                    break;
                }
            }
            return name.append(']').toString();
        }
        if (!simple.isEmpty() && !type.getName().startsWith(DECLARATIVE_PACKAGE)) {
            return simple;
        }
        String debug;
        try {
            debug = behavior instanceof BehaviorControl<?> control ? control.debugString() : type.getName();
        } catch (RuntimeException exception) {
            debug = type.getName();
        }
        return "OneShot:" + BehaviorNames.lambdaOwners(debug);
    }

    private static String unreachedSample(LivingEntity villager, Set<BlockPos> targets, Path path) {
        BlockPos target = path != null && path.getTarget() != null ? path.getTarget()
                : targets.isEmpty() ? null : targets.iterator().next();
        Node end = path == null ? null : path.getEndNode();
        String targetText = target == null ? "?" : target.getX() + "," + target.getY() + "," + target.getZ()
                + " (manhattan " + target.distManhattan(villager.blockPosition()) + ")";
        String endText = end == null ? "none" : end.x + "," + end.y + "," + end.z;
        return "target " + targetText + ", path end " + endText
                + ", nodes " + (path == null ? 0 : path.getNodeCount())
                + (targets.size() > 1 ? ", targets " + targets.size() : "");
    }

    private static String simpleName(String className) {
        String name = className.substring(className.lastIndexOf('.') + 1);
        int lambda = name.indexOf("$$Lambda");
        return lambda >= 0 ? name.substring(0, lambda) : name;
    }
}
