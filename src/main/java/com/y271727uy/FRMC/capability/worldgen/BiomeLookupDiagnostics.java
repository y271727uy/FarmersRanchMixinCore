package com.y271727uy.FRMC.capability.worldgen;

import com.y271727uy.FRMC.config.WorldgenDiagnosticsSwitch;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;

/**
 * 只测量、不改变行为：统计 {@code MultiNoiseBiomeSource.getNoiseBiome} 对同一生物群系格的重复查询，
 * 用来判断"格坐标 → 生物群系"缓存是否值得做。
 *
 * <p>同时模拟三种候选缓存：每线程 4K 槽、全局共享 64K 槽、全局共享 1M 槽（都是直接映射、冲突即覆盖）。
 * 1M 槽近似"短期内的全部重复"，是任何实际缓存命中率的上限参考。
 * 调用分两类：区块在 BIOMES 阶段填充自己的群系（fill），以及其他所有调用方（other）。
 *
 * <p>只有在 {@link WorldgenDiagnosticsSwitch#biomeLookupDiagnostics()} 打开时，相关 mixin 才会被织入。
 */
public final class BiomeLookupDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger("FRMC-BiomeLookup");

    private static final int FILL = 0;
    private static final int OTHER = 1;
    private static final String[] CONTEXT_NAMES = {"fill", "other"};

    private static final int THREAD_TABLE_BITS = 12;
    private static final int SHARED_SMALL_BITS = 16;
    private static final int SHARED_LARGE_BITS = 20;
    private static final long EMPTY = Long.MIN_VALUE;

    private static final int TIMING_SAMPLE = 32;
    private static final int CALLER_SAMPLE = 256;
    private static final int TOP_CALLERS = 6;
    private static final long REPORT_INTERVAL_NS = TimeUnit.SECONDS.toNanos(10);

    private static final Counters WINDOW = new Counters();
    private static final long[][] TOTALS = new long[2][Counters.FIELDS];

    /** 调用方 → {抽样调用次数, 其中命中 1M 模拟缓存的次数}。 */
    private static final Map<String, LongAdder[]> CALLERS = new ConcurrentHashMap<>();
    private static final StackWalker WALKER = StackWalker.getInstance();

    private static final ThreadLocal<ThreadState> STATE = ThreadLocal.withInitial(ThreadState::new);

    private static long lastReport = System.nanoTime();

    private BiomeLookupDiagnostics() {
    }

    public static void register() {
        if (!WorldgenDiagnosticsSwitch.biomeLookupDiagnostics()) {
            return;
        }
        MinecraftForge.EVENT_BUS.addListener(BiomeLookupDiagnostics::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(BiomeLookupDiagnostics::onServerStopping);
        LOGGER.info("Biome lookup diagnostics enabled: measuring MultiNoiseBiomeSource.getNoiseBiome, reporting every {} s",
            TimeUnit.NANOSECONDS.toSeconds(REPORT_INTERVAL_NS));
    }

    // ---- 由 mixin 调用 ----

    public static void enterChunkFill() {
        STATE.get().fillDepth++;
    }

    public static void exitChunkFill() {
        ThreadState state = STATE.get();
        if (state.fillDepth > 0) {
            state.fillDepth--;
        }
    }

    /** 查询前调用；返回值传回 {@link #afterLookup}。返回 0 表示这次不计时。 */
    public static long beforeLookup(Object source, int quartX, int quartY, int quartZ) {
        ThreadState state = STATE.get();
        int context = state.fillDepth > 0 ? FILL : OTHER;
        long key = key(source, quartX, quartY, quartZ);

        boolean threadHit = probe(state.table, THREAD_TABLE_BITS, key);
        boolean smallHit = probeShared(SharedTables.SMALL, SHARED_SMALL_BITS, key);
        boolean largeHit = probeShared(SharedTables.LARGE, SHARED_LARGE_BITS, key);

        Counters c = WINDOW;
        c.calls[context].increment();
        if (threadHit) {
            c.threadHits[context].increment();
        }
        if (smallHit) {
            c.sharedSmallHits[context].increment();
        }
        if (largeHit) {
            c.sharedLargeHits[context].increment();
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (context == OTHER && random.nextInt(CALLER_SAMPLE) == 0) {
            recordCaller(largeHit);
        }
        if (random.nextInt(TIMING_SAMPLE) != 0) {
            return 0L;
        }
        state.timedContext = context;
        long start = System.nanoTime();
        return start == 0L ? 1L : start;
    }

    public static void afterLookup(long start) {
        if (start == 0L) {
            return;
        }
        long elapsed = System.nanoTime() - start;
        int context = STATE.get().timedContext;
        WINDOW.timedCalls[context].increment();
        WINDOW.timedNs[context].add(elapsed);
    }

    // ---- 模拟缓存 ----

    private static long key(Object source, int x, int y, int z) {
        long packed = ((long) (x & 0x3FFFFF) << 42) | ((long) (z & 0x3FFFFF) << 20) | (y & 0xFFFFF);
        long key = packed ^ (System.identityHashCode(source) * 0x9E3779B97F4A7C15L);
        return key == EMPTY ? key ^ 1L : key;
    }

    private static int slot(long key, int bits) {
        return (int) ((key * 0xC2B2AE3D27D4EB4FL) >>> (64 - bits));
    }

    private static boolean probe(long[] table, int bits, long key) {
        int slot = slot(key, bits);
        if (table[slot] == key) {
            return true;
        }
        table[slot] = key;
        return false;
    }

    private static boolean probeShared(AtomicLongArray table, int bits, long key) {
        int slot = slot(key, bits);
        if (table.get(slot) == key) {
            return true;
        }
        table.lazySet(slot, key);
        return false;
    }

    private static AtomicLongArray emptyShared(int bits) {
        long[] init = new long[1 << bits];
        Arrays.fill(init, EMPTY);
        return new AtomicLongArray(init);
    }

    // ---- 调用方抽样 ----

    private static void recordCaller(boolean repeated) {
        String caller = WALKER.walk(frames -> frames
            .filter(frame -> !isInternalFrame(frame.getClassName()))
            .findFirst()
            .map(frame -> simpleName(frame.getClassName()) + "#" + frame.getMethodName())
            .orElse("<unknown>"));
        LongAdder[] counts = CALLERS.computeIfAbsent(caller, ignored -> new LongAdder[]{new LongAdder(), new LongAdder()});
        counts[0].increment();
        if (repeated) {
            counts[1].increment();
        }
    }

    private static boolean isInternalFrame(String className) {
        return className.startsWith("com.y271727uy.FRMC.")
            || className.startsWith("net.minecraft.world.level.biome.")
            || className.startsWith("java.")
            || className.startsWith("jdk.")
            || className.contains("$$Lambda");
    }

    private static String simpleName(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }

    // ---- 报告 ----

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastReport < REPORT_INTERVAL_NS) {
            return;
        }
        lastReport = now;
        long[][] window = WINDOW.drain();
        for (int context = 0; context < 2; context++) {
            for (int field = 0; field < Counters.FIELDS; field++) {
                TOTALS[context][field] += window[context][field];
            }
        }
        if (window[FILL][Counters.CALLS] + window[OTHER][Counters.CALLS] == 0L) {
            return;
        }
        log("10s window", window);
        logCallers();
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        long[][] window = WINDOW.drain();
        for (int context = 0; context < 2; context++) {
            for (int field = 0; field < Counters.FIELDS; field++) {
                TOTALS[context][field] += window[context][field];
            }
        }
        if (TOTALS[FILL][Counters.CALLS] + TOTALS[OTHER][Counters.CALLS] > 0L) {
            log("session total", TOTALS);
            logCallers();
        }
        for (long[] row : TOTALS) {
            Arrays.fill(row, 0L);
        }
    }

    private static void log(String label, long[][] data) {
        StringBuilder line = new StringBuilder("Biome lookups [").append(label).append(']');
        double saveableMs = 0.0;
        for (int context = 0; context < 2; context++) {
            long[] row = data[context];
            long calls = row[Counters.CALLS];
            long avgNs = row[Counters.TIMED_CALLS] == 0L ? 0L : row[Counters.TIMED_NS] / row[Counters.TIMED_CALLS];
            saveableMs += row[Counters.SHARED_SMALL_HITS] * (double) avgNs / 1_000_000.0;
            line.append(" | ").append(CONTEXT_NAMES[context])
                .append(": calls=").append(calls)
                .append(" avgNs=").append(avgNs)
                .append(" hit% thread4K=").append(percent(row[Counters.THREAD_HITS], calls))
                .append(" shared64K=").append(percent(row[Counters.SHARED_SMALL_HITS], calls))
                .append(" shared1M=").append(percent(row[Counters.SHARED_LARGE_HITS], calls));
        }
        line.append(" | est. saveable with shared64K=").append(String.format("%.1f", saveableMs)).append(" ms");
        LOGGER.info(line.toString());
    }

    private static void logCallers() {
        if (CALLERS.isEmpty()) {
            return;
        }
        List<Map.Entry<String, long[]>> snapshot = new ArrayList<>();
        for (Map.Entry<String, LongAdder[]> entry : CALLERS.entrySet()) {
            snapshot.add(Map.entry(entry.getKey(),
                new long[]{entry.getValue()[0].sumThenReset(), entry.getValue()[1].sumThenReset()}));
        }
        CALLERS.clear();
        snapshot.removeIf(entry -> entry.getValue()[0] == 0L);
        snapshot.sort((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]));
        StringBuilder line = new StringBuilder("Biome lookup callers (other, sampled 1/").append(CALLER_SAMPLE).append("):");
        for (int i = 0; i < Math.min(TOP_CALLERS, snapshot.size()); i++) {
            long[] counts = snapshot.get(i).getValue();
            line.append(' ').append(snapshot.get(i).getKey())
                .append("=").append(counts[0])
                .append("(repeat ").append(percent(counts[1], counts[0])).append("%)");
        }
        LOGGER.info(line.toString());
    }

    private static String percent(long part, long total) {
        return total == 0L ? "0.0" : String.format("%.1f", part * 100.0 / total);
    }

    /** 约 8.5 MB，只在第一次查询（即诊断 mixin 已织入）时才分配。 */
    private static final class SharedTables {
        private static final AtomicLongArray SMALL = emptyShared(SHARED_SMALL_BITS);
        private static final AtomicLongArray LARGE = emptyShared(SHARED_LARGE_BITS);
    }

    private static final class ThreadState {
        private final long[] table = new long[1 << THREAD_TABLE_BITS];
        private int fillDepth;
        private int timedContext;

        private ThreadState() {
            Arrays.fill(table, EMPTY);
        }
    }

    private static final class Counters {
        private static final int CALLS = 0;
        private static final int THREAD_HITS = 1;
        private static final int SHARED_SMALL_HITS = 2;
        private static final int SHARED_LARGE_HITS = 3;
        private static final int TIMED_CALLS = 4;
        private static final int TIMED_NS = 5;
        private static final int FIELDS = 6;

        private final LongAdder[] calls = adders();
        private final LongAdder[] threadHits = adders();
        private final LongAdder[] sharedSmallHits = adders();
        private final LongAdder[] sharedLargeHits = adders();
        private final LongAdder[] timedCalls = adders();
        private final LongAdder[] timedNs = adders();

        private static LongAdder[] adders() {
            return new LongAdder[]{new LongAdder(), new LongAdder()};
        }

        private long[][] drain() {
            long[][] out = new long[2][FIELDS];
            for (int context = 0; context < 2; context++) {
                out[context][CALLS] = calls[context].sumThenReset();
                out[context][THREAD_HITS] = threadHits[context].sumThenReset();
                out[context][SHARED_SMALL_HITS] = sharedSmallHits[context].sumThenReset();
                out[context][SHARED_LARGE_HITS] = sharedLargeHits[context].sumThenReset();
                out[context][TIMED_CALLS] = timedCalls[context].sumThenReset();
                out[context][TIMED_NS] = timedNs[context].sumThenReset();
            }
            return out;
        }
    }
}
