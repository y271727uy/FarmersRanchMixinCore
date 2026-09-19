package com.y271727uy.FRMC.capability.blockchecking.server;

import com.y271727uy.FRMC.config.BlockCheckingConfig;
import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureSnapshot;
import com.y271727uy.FRMC.capability.blockchecking.model.BlockCheckingReport;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkKind;
import com.y271727uy.FRMC.network.BlockCheckingNetwork;
import com.y271727uy.FRMC.capability.blockchecking.runtime.BlockCheckingSession;
import com.y271727uy.FRMC.capability.blockchecking.runtime.WorkContextTracker;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/** Server event bridge for bounded block-checking collection. No sampler operation touches game state. */
public final class BlockCheckingRuntime {
    private static final BlockCheckingSession SESSION = new BlockCheckingSession();
    private static final WorkContextTracker CONTEXT = new WorkContextTracker();
    private static final ChunkPressureSnapshotCollector CHUNKS = new ChunkPressureSnapshotCollector();
    private static volatile ChunkPressureSnapshot lastChunks = new ChunkPressureSnapshot(0L, List.of(), true);
    private static volatile boolean samplerRunning;
    private static volatile Thread samplerThread;
    private static volatile long tickStartedAtNanos;
    private static volatile net.minecraft.server.MinecraftServer server;
    private static int chunkSnapshotCountdown;

    private BlockCheckingRuntime() {
    }

    public static boolean start(int seconds) {
        if (!BlockCheckingConfig.enabled()) {
            return false;
        }
        int duration = Math.max(1, Math.min(seconds, BlockCheckingConfig.maximumSessionSeconds()));
        boolean started = SESSION.start(System.nanoTime(), duration);
        if (started) {
            ensureSampler();
            chunkSnapshotCountdown = 0;
            publishImmediateSnapshot();
        }
        return started;
    }

    public static boolean stop() {
        boolean stopped = SESSION.stop(System.nanoTime(), BlockCheckingConfig.outputEntryLimit());
        if (stopped) {
            BlockCheckingNetwork.publishReport(SESSION.lastReport());
        }
        return stopped;
    }

    public static boolean startFor(net.minecraft.server.level.ServerPlayer player) {
        if (player != null && player.getServer() != null) {
            server = player.getServer();
        }
        boolean started = start(BlockCheckingConfig.defaultSessionSeconds());
        if (!started) {
            publishImmediateSnapshot();
        }
        return started;
    }

    public static boolean active() {
        return SESSION.active();
    }

    public static BlockCheckingReport lastReport() {
        return SESSION.lastReport();
    }

    public static ChunkPressureSnapshot lastChunks() {
        return lastChunks;
    }

    /**
     * Force one chunk row into the last snapshot and republish. Does not start a session.
     */
    public static void inspectChunk(net.minecraft.server.level.ServerPlayer player, String dimension, long packedChunk) {
        if (player != null && player.getServer() != null) {
            server = player.getServer();
        }
        inspectChunk(dimension, packedChunk);
    }

    public static void inspectChunk(String dimension, long packedChunk) {
        net.minecraft.server.MinecraftServer current = server;
        if (current == null || dimension == null || dimension.isEmpty()) {
            return;
        }
        ChunkPressureSnapshot one = CHUNKS.collectOne(current, dimension, packedChunk);
        if (one.entries().isEmpty()) {
            return;
        }
        lastChunks = ChunkPressureSnapshot.merge(lastChunks, one.entries().get(0), System.currentTimeMillis(),
                BlockCheckingConfig.outputEntryLimit());
        BlockCheckingNetwork.publishSnapshot(lastChunks);
    }

    /**
     * Drops one published overlay row immediately. An active session collect may put a tracker
     * chunk back on the next snapshot period.
     */
    public static void uninspectChunk(net.minecraft.server.level.ServerPlayer player, String dimension, long packedChunk) {
        if (player != null && player.getServer() != null) {
            server = player.getServer();
        }
        uninspectChunk(dimension, packedChunk);
    }

    public static void uninspectChunk(String dimension, long packedChunk) {
        if (dimension == null || dimension.isEmpty()) {
            return;
        }
        ChunkPressureSnapshot current = lastChunks;
        ChunkPressureSnapshot next = ChunkPressureSnapshot.without(current, dimension, packedChunk,
                System.currentTimeMillis());
        if (next.entries().size() == current.entries().size()) {
            return;
        }
        lastChunks = next;
        BlockCheckingNetwork.publishSnapshot(lastChunks);
    }

    public static net.minecraft.server.MinecraftServer server() {
        return server;
    }

    /** Mixins may call this without timing every work entry. */
    public static long enterWork(WorkKind kind) {
        return SESSION.active() ? CONTEXT.enter(kind) : 0L;
    }

    /** Mixins may call this with the token returned by {@link #enterWork(WorkKind)}. */
    public static boolean exitWork(long token) {
        return token != 0L && CONTEXT.exit(token);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            server = event.getServer();
            if (SESSION.active()) {
                tickStartedAtNanos = System.nanoTime();
            }
            return;
        }
        if (!BlockCheckingConfig.enabled()) {
            return;
        }
        if (!SESSION.active()) {
            return;
        }
        SESSION.recordTick(System.nanoTime() - tickStartedAtNanos);
        if (++chunkSnapshotCountdown >= BlockCheckingConfig.chunkSnapshotPeriodTicks()) {
            chunkSnapshotCountdown = 0;
            lastChunks = CHUNKS.collect(event.getServer(), BlockCheckingConfig.outputEntryLimit());
            BlockCheckingNetwork.publishSnapshot(lastChunks);
        }
        if (SESSION.finishIfDue(System.nanoTime(), BlockCheckingConfig.outputEntryLimit())) {
            BlockCheckingNetwork.publishReport(SESSION.lastReport());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        shutdown();
    }

    public static void clear() {
        SESSION.clear();
        CONTEXT.clear();
        lastChunks = new ChunkPressureSnapshot(0L, List.of(), true);
        chunkSnapshotCountdown = 0;
    }

    public static void shutdown() {
        BlockCheckingNetwork.clearSubscriptions();
        samplerRunning = false;
        Thread thread = samplerThread;
        samplerThread = null;
        if (thread != null) {
            thread.interrupt();
        }
        server = null;
        clear();
    }

    private static void publishImmediateSnapshot() {
        net.minecraft.server.MinecraftServer current = server;
        if (current == null) {
            return;
        }
        lastChunks = CHUNKS.collect(current, BlockCheckingConfig.outputEntryLimit());
        BlockCheckingNetwork.publishSnapshot(lastChunks);
    }

    private static synchronized void ensureSampler() {
        if (samplerRunning) {
            return;
        }
        samplerRunning = true;
        Thread thread = new Thread(BlockCheckingRuntime::sampleLoop, "FRMC blockchecking sampler");
        thread.setDaemon(true);
        samplerThread = thread;
        thread.start();
    }

    private static void sampleLoop() {
        while (samplerRunning) {
            try {
                if (SESSION.active()) {
                    SESSION.recordSample(CONTEXT.capture(System.nanoTime()));
                }
            } catch (RuntimeException ignored) {
                // A sampler failure must never reach or disrupt the server thread.
            }
            try {
                Thread.sleep(BlockCheckingConfig.sampleIntervalMillis());
            } catch (InterruptedException ignored) {
                // Re-check samplerRunning so shutdown remains prompt.
            }
        }
    }
}
