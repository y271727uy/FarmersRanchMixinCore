package com.y271727uy.FRMC.integration.computatio;

import com.y271727uy.computatio.algorithm.fastpath.model.GridPos;
import com.y271727uy.computatio.algorithm.fastpath.model.MovementMode;
import com.y271727uy.computatio.algorithm.fastpath.model.PathCapabilities;
import com.y271727uy.computatio.algorithm.fastpath.model.PathIntent;
import com.y271727uy.computatio.algorithm.fastpath.model.PathRequest;
import com.y271727uy.computatio.algorithm.fastpath.model.PathResult;
import com.y271727uy.computatio.algorithm.fastpath.model.PathVector;
import com.y271727uy.computatio.algorithm.fastpath.model.PathWaypoint;
import com.y271727uy.computatio.algorithm.fastpath.model.TraversalCell;
import com.y271727uy.computatio.algorithm.fastpath.model.TraversalSnapshot;
import com.y271727uy.computatio.api.fastpath.FastPath;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared, one-tick water samples for fish that fit inside a single block. */
final class WaterFastPath {
    private static final int TILE_SIZE = 4;
    private static final int MAX_SAMPLES_PER_TICK = 4;
    private static final int MAX_CELLS = 1024;
    private static final int WORKER_COUNT = Math.min(2, Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    private static final PathCapabilities SWIMMING = new PathCapabilities(false, false, false, false);
    private static final Map<ServerLevel, TickSamples> SAMPLES = new WeakHashMap<>();
    private static final ThreadPoolExecutor WORKERS = new ThreadPoolExecutor(
            WORKER_COUNT, WORKER_COUNT,
            30L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(16), task -> {
                Thread worker = new Thread(task, "FRMC water pathfinding");
                worker.setDaemon(true);
                return worker;
            });

    static {
        WORKERS.allowCoreThreadTimeOut(true);
    }

    private WaterFastPath() {
    }

    static CompletableFuture<PathResult> submit(ServerLevel level, BlockPos start, BlockPos target) {
        if (WORKERS.getQueue().remainingCapacity() == 0) {
            return null;
        }
        Bounds bounds = Bounds.around(start, target);
        if (bounds.volume() > MAX_CELLS || !loaded(level, bounds.min(), bounds.max())) {
            return null;
        }
        TickSamples tickSamples;
        synchronized (SAMPLES) {
            tickSamples = SAMPLES.computeIfAbsent(level, ignored -> new TickSamples());
        }
        TraversalSnapshot snapshot = tickSamples.get(level, bounds);
        GridPos from = grid(start);
        GridPos to = grid(target);
        if (snapshot == null || snapshot.cell(from) == null || snapshot.cell(to) == null) {
            return null;
        }
        PathRequest request = new PathRequest(snapshot.worldId(), from,
                new PathVector(target.getX() + 0.5, target.getY(), target.getZ() + 0.5),
                PathIntent.WALK, SWIMMING);
        try {
            return FastPath.findAsync(request, snapshot, WORKERS);
        } catch (RejectedExecutionException ignored) {
            return null;
        }
    }

    static boolean clear(ServerLevel level, Mob mob, Vec3 from, Vec3 to) {
        AABB body = mob.getBoundingBox().move(from.subtract(mob.position()));
        AABB sweep = body.expandTowards(to.subtract(from));
        BlockPos min = BlockPos.containing(sweep.minX + 1.0E-7, sweep.minY + 1.0E-7, sweep.minZ + 1.0E-7);
        BlockPos max = BlockPos.containing(sweep.maxX - 1.0E-7, sweep.maxY - 1.0E-7, sweep.maxZ - 1.0E-7);
        if (!loaded(level, min, max)) {
            return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            var state = level.getBlockState(pos);
            if (!isSourceWater(state) || (pos.getY() == max.getY()
                    && sweep.maxY > pos.getY() + state.getFluidState().getHeight(level, pos) + 1.0E-7)) {
                return false;
            }
        }
        return !level.getBlockCollisions(mob, sweep).iterator().hasNext();
    }

    private static boolean isSourceWater(BlockState state) {
        return state.is(Blocks.WATER) && state.getValue(LiquidBlock.LEVEL) == 0;
    }

    static BlockPos swimmingCell(PathWaypoint waypoint) {
        if (waypoint.mode() != MovementMode.SWIM) {
            return null;
        }
        GridPos cell = waypoint.blockPosition();
        return new BlockPos(cell.x(), cell.y(), cell.z());
    }

    private static boolean loaded(ServerLevel level, BlockPos min, BlockPos max) {
        BlockPos low = min.offset(-1, -1, -1);
        BlockPos high = max.offset(1, 1, 1);
        return !level.isOutsideBuildHeight(low) && !level.isOutsideBuildHeight(high)
                && level.getWorldBorder().isWithinBounds(low) && level.getWorldBorder().isWithinBounds(high)
                && level.hasChunksAt(low, high);
    }

    private static GridPos grid(BlockPos pos) {
        return new GridPos(pos.getX(), pos.getY(), pos.getZ());
    }

    private record Bounds(BlockPos min, BlockPos max) {
        private static Bounds around(BlockPos start, BlockPos end) {
            return new Bounds(new BlockPos(tile(Math.min(start.getX(), end.getX())) - 1,
                    tile(Math.min(start.getY(), end.getY())) - 1, tile(Math.min(start.getZ(), end.getZ())) - 1),
                    new BlockPos(tile(Math.max(start.getX(), end.getX())) + TILE_SIZE,
                            tile(Math.max(start.getY(), end.getY())) + TILE_SIZE,
                            tile(Math.max(start.getZ(), end.getZ())) + TILE_SIZE));
        }

        private static int tile(int coordinate) {
            return Math.floorDiv(coordinate, TILE_SIZE) * TILE_SIZE;
        }

        private int volume() {
            return (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        }
    }

    private static final class TickSamples {
        private final Map<Bounds, TraversalSnapshot> snapshots = new HashMap<>();
        private long tick = Long.MIN_VALUE;

        private TraversalSnapshot get(ServerLevel level, Bounds bounds) {
            if (tick != level.getGameTime()) {
                tick = level.getGameTime();
                snapshots.clear();
            }
            TraversalSnapshot cached = snapshots.get(bounds);
            if (cached != null || snapshots.size() >= MAX_SAMPLES_PER_TICK) {
                return cached;
            }
            // FastPath uses the request endpoints. The snapshot describes the shared volume.
            GridPos min = grid(bounds.min());
            GridPos max = grid(bounds.max());
            TraversalSnapshot.Builder builder = TraversalSnapshot.builder(level.dimension().location().toString(),
                    min, max, min.y(), max.y(), tick, true);
            for (BlockPos pos : BlockPos.betweenClosed(bounds.min(), bounds.max())) {
                if (isSourceWater(level.getBlockState(pos))) {
                    builder.addCell(new TraversalCell(pos.getX(), pos.getY(), pos.getZ(), pos.getY(),
                            true, false, false, 1.0));
                }
            }
            TraversalSnapshot snapshot = builder.build();
            snapshots.put(bounds, snapshot);
            return snapshot;
        }
    }
}
