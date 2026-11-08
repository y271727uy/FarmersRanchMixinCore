package com.y271727uy.FRMC.integration.computatio;

import com.y271727uy.computatio.algorithm.fastpath.model.GridPos;
import com.y271727uy.computatio.algorithm.fastpath.model.MovementMode;
import com.y271727uy.computatio.algorithm.fastpath.model.PathCapabilities;
import com.y271727uy.computatio.algorithm.fastpath.model.PathIntent;
import com.y271727uy.computatio.algorithm.fastpath.model.PathRequest;
import com.y271727uy.computatio.algorithm.fastpath.model.PathResult;
import com.y271727uy.computatio.algorithm.fastpath.model.PathVector;
import com.y271727uy.computatio.algorithm.fastpath.model.TraversalSnapshot;
import com.y271727uy.computatio.api.fastpath.FastPath;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Supplies temporary waypoints only for an obstructed delivery. */
public final class CrowDeliveryDetour {
    private static final int OBSERVE_TICKS = 20;
    private static final int RETRY_TICKS = 60;
    private static final int MAX_DISTANCE = 12;
    private static final int MAX_CELLS = 768;
    private static final int MARGIN = 2;
    private static final PathCapabilities FLIGHT = new PathCapabilities(false, true, false, false);
    private static final Map<ServerLevel, Long> LAST_SEARCH = new WeakHashMap<>();

    private ItemFrame frame;
    private Vec3 observedPosition;
    private long observeAt;
    private long retryAt;
    private ArrayDeque<Vec3> route;

    public Vec3 waypoint(Mob crow, ItemFrame target) {
        if (!(crow.level() instanceof ServerLevel level) || !level.getServer().isSameThread()
                || target == null || !target.isAlive() || crow.distanceToSqr(target) < 4.0) {
            reset();
            return null;
        }
        long now = level.getGameTime();
        Vec3 position = crow.position();
        if (frame != target) {
            reset();
            frame = target;
            observedPosition = position;
            observeAt = now + OBSERVE_TICKS;
            return null;
        }
        if (route != null) {
            while (!route.isEmpty() && position.distanceToSqr(route.getFirst()) < 0.36) {
                route.removeFirst();
            }
            if (!route.isEmpty() && now < retryAt && clear(level, crow, position, route.getFirst())) {
                return route.getFirst();
            }
            route = null;
        }
        if (now < observeAt || now < retryAt) {
            return null;
        }
        boolean stalled = position.distanceToSqr(observedPosition) < 0.25;
        observedPosition = position;
        observeAt = now + OBSERVE_TICKS;
        if (!stalled) {
            return null;
        }

        Vec3 approach = target.position().add(Vec3.atLowerCornerOf(target.getDirection().getNormal()).scale(0.8));
        retryAt = now + RETRY_TICKS;
        route = search(level, crow, approach);
        return route == null ? null : route.getFirst();
    }

    public void reset() {
        frame = null;
        route = null;
        retryAt = 0;
    }

    private static ArrayDeque<Vec3> search(ServerLevel level, Mob crow, Vec3 approach) {
        Vec3 origin = crow.position();
        BlockPos start = crow.blockPosition();
        BlockPos end = BlockPos.containing(approach);
        if (start.distManhattan(end) > MAX_DISTANCE) {
            return null;
        }
        BlockPos min = new BlockPos(Math.min(start.getX(), end.getX()) - MARGIN,
                Math.max(level.getMinBuildHeight(), Math.min(start.getY(), end.getY()) - MARGIN),
                Math.min(start.getZ(), end.getZ()) - MARGIN);
        BlockPos max = new BlockPos(Math.max(start.getX(), end.getX()) + MARGIN,
                Math.min(level.getMaxBuildHeight() - 1, Math.max(start.getY(), end.getY()) + MARGIN),
                Math.max(start.getZ(), end.getZ()) + MARGIN);
        int cells = (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        if (min.getY() > max.getY() || cells > MAX_CELLS || !loaded(level, min, max)
                || !level.getBlockState(start).isAir() || !level.getBlockState(end).isAir()
                || level.clip(new ClipContext(crow.getEyePosition(), approach, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, crow)).getType() == HitResult.Type.MISS) {
            return null;
        }
        long now = level.getGameTime();
        synchronized (LAST_SEARCH) {
            if (LAST_SEARCH.getOrDefault(level, Long.MIN_VALUE) == now) {
                return null;
            }
            LAST_SEARCH.put(level, now);
        }

        String world = level.dimension().location().toString();
        PathRequest request = new PathRequest(world, grid(start),
                new PathVector(approach.x, approach.y, approach.z), PathIntent.WALK, FLIGHT);
        TraversalSnapshot.Builder snapshot = TraversalSnapshot.builder(world, request.start(), request.targetBlock(),
                min.getY(), max.getY(), now, true);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(pos).isAir()) {
                snapshot.addBodyPassage(pos.getX(), pos.getY(), pos.getZ());
            }
        }
        PathResult result = FastPath.find(request, snapshot.build());
        if (result.status() != PathResult.Status.EXACT_TARGET) {
            return null;
        }
        ArrayDeque<Vec3> waypoints = new ArrayDeque<>();
        Vec3 previous = origin;
        for (var waypoint : result.waypoints()) {
            GridPos cell = waypoint.blockPosition();
            if (waypoint.mode() != MovementMode.FLY) {
                return null;
            }
            if (waypoints.isEmpty() && cell.equals(request.start())) {
                continue;
            }
            Vec3 next = cell.equals(request.targetBlock()) ? approach : new Vec3(cell.x() + 0.5, cell.y(), cell.z() + 0.5);
            if (!clear(level, crow, previous, next)) {
                return null;
            }
            waypoints.addLast(next);
            previous = next;
        }
        return waypoints.isEmpty() ? null : waypoints;
    }

    private static boolean clear(ServerLevel level, Mob crow, Vec3 from, Vec3 to) {
        AABB sweep = crow.getBoundingBox().move(from.subtract(crow.position())).expandTowards(to.subtract(from));
        BlockPos min = BlockPos.containing(sweep.minX + 1.0E-7, sweep.minY + 1.0E-7, sweep.minZ + 1.0E-7);
        BlockPos max = BlockPos.containing(sweep.maxX - 1.0E-7, sweep.maxY - 1.0E-7, sweep.maxZ - 1.0E-7);
        return loaded(level, min, max) && !level.getBlockCollisions(crow, sweep).iterator().hasNext()
                && level.getBlockStatesIfLoaded(sweep.deflate(1.0E-7)).noneMatch(state -> !state.getFluidState().isEmpty());
    }

    private static boolean loaded(ServerLevel level, BlockPos min, BlockPos max) {
        BlockPos low = min.offset(-1, 0, -1);
        BlockPos high = max.offset(1, 0, 1);
        return !level.isOutsideBuildHeight(low) && !level.isOutsideBuildHeight(high)
                && level.getWorldBorder().isWithinBounds(low) && level.getWorldBorder().isWithinBounds(high)
                && level.hasChunksAt(low, high);
    }

    private static GridPos grid(BlockPos pos) {
        return new GridPos(pos.getX(), pos.getY(), pos.getZ());
    }
}
