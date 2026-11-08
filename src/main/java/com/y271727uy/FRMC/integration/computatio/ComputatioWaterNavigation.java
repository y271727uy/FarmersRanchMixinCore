package com.y271727uy.FRMC.integration.computatio;

import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.mixin.minecraft.pathfinding.PathNavigationAccessor;
import com.y271727uy.computatio.algorithm.fastpath.model.PathResult;
import com.y271727uy.computatio.algorithm.fastpath.model.PathWaypoint;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/** Replaces coordinate-based short swims while keeping the vanilla water path follower. */
public final class ComputatioWaterNavigation extends WaterBoundPathNavigation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_DISTANCE = 10;
    private static final int MAX_WAIT_TICKS = 6;
    private static final int RETRY_DELAY_TICKS = 20;
    private final ServerLevel serverLevel;
    private Pending pending;
    private BlockPos retryTarget;
    private long retryAt;

    public ComputatioWaterNavigation(Mob mob, ServerLevel level) {
        super(mob, level);
        this.serverLevel = level;
    }

    @Override
    public boolean moveTo(double x, double y, double z, double speed) {
        BlockPos target = BlockPos.containing(x, y, z);
        if (pending != null && pending.target.equals(target)) {
            pending.speed = speed;
            return true;
        }
        cancelPending();
        if (path != null && !path.isDone() && target.equals(getTargetPos())) {
            return super.moveTo(path, speed);
        }
        BlockPos start = mob.blockPosition();
        double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (!serverLevel.getServer().isSameThread() || !eligible()
                || start.distManhattan(target) > MAX_DISTANCE || start.distSqr(target) >= followRange * followRange
                || target.equals(retryTarget) && serverLevel.getGameTime() < retryAt) {
            return super.moveTo(x, y, z, speed);
        }
        if (WaterFastPath.clear(serverLevel, mob, mob.position(), Vec3.atBottomCenterOf(target))) {
            return install(new SwimPath(List.of(node(target)), target, start), speed);
        }
        CompletableFuture<PathResult> search = WaterFastPath.submit(serverLevel, start, target);
        if (search == null) {
            return fallback(target, speed);
        }
        super.stop();
        // Catfish's aquatic controller can keep steering toward the previous wanted position.
        mob.getMoveControl().strafe(0.0F, 0.0F);
        pending = new Pending(start, target, speed, serverLevel.getGameTime() + MAX_WAIT_TICKS, search);
        return true;
    }

    @Override
    public boolean moveTo(Entity target, double speed) {
        cancelPending();
        return super.moveTo(target, speed);
    }

    @Override
    public boolean moveTo(Path newPath, double speed) {
        cancelPending();
        return super.moveTo(newPath, speed);
    }

    @Override
    public boolean isDone() {
        return pending == null && super.isDone();
    }

    @Override
    public void stop() {
        cancelPending();
        super.stop();
    }

    @Override
    public void tick() {
        Pending request = pending;
        if (request != null) {
            if (!mob.isAlive() || mob.getNavigation() != this) {
                stop();
                return;
            }
            if (eligible() && request.start.equals(mob.blockPosition())
                    && serverLevel.getGameTime() < request.deadline && !request.search.isDone()) {
                return;
            }
            pending = null;
            SwimPath route = eligible() && request.start.equals(mob.blockPosition())
                    ? completedRoute(request) : null;
            request.search.cancel(false);
            if (route == null || !install(route, request.speed)) {
                fallback(request.target, request.speed);
            }
        }
        super.tick();
    }

    @Override
    public void recomputePath() {
        // Pending routes are checked against live terrain before they are installed.
        if (pending == null) {
            super.recomputePath();
        }
    }

    @Override
    public boolean shouldRecomputePath(BlockPos changedBlock) {
        if (path instanceof SwimPath swim && !swim.isDone() && !hasDelayedRecomputation && swim.crosses(changedBlock)) {
            return true;
        }
        return super.shouldRecomputePath(changedBlock);
    }

    @Override
    public boolean canCutCorner(BlockPathTypes type) {
        return !(path instanceof SwimPath) && super.canCutCorner(type);
    }

    private boolean eligible() {
        return mob.isInWater() && mob.getBbWidth() < 1.0F && mob.getBbHeight() <= 1.0F;
    }

    private SwimPath completedRoute(Pending request) {
        PathResult result;
        try {
            result = request.search.getNow(null);
        } catch (CompletionException exception) {
            LOGGER.debug("Computatio water path search failed", exception);
            return null;
        }
        if (result == null || result.status() != PathResult.Status.EXACT_TARGET) {
            return null;
        }
        List<Node> nodes = new ArrayList<>(result.waypoints().size());
        Vec3 from = mob.position();
        for (PathWaypoint waypoint : result.waypoints()) {
            BlockPos cell = WaterFastPath.swimmingCell(waypoint);
            if (cell == null) {
                return null;
            }
            if (nodes.isEmpty() && cell.equals(request.start)) {
                continue;
            }
            Vec3 to = Vec3.atBottomCenterOf(cell);
            if (!WaterFastPath.clear(serverLevel, mob, from, to)) {
                return null;
            }
            nodes.add(node(cell));
            from = to;
        }
        if (nodes.isEmpty()) {
            return null;
        }
        return new SwimPath(nodes, request.target, request.start);
    }

    private boolean install(SwimPath route, double speed) {
        if (!super.moveTo(route, speed)) {
            return false;
        }
        PathNavigationAccessor state = (PathNavigationAccessor) (Object) this;
        state.frmc$setTargetPos(route.getTarget());
        state.frmc$setReachRange(1);
        state.frmc$resetStuckTimeout();
        hasDelayedRecomputation = false;
        return true;
    }

    private boolean fallback(BlockPos target, double speed) {
        retryTarget = target;
        retryAt = serverLevel.getGameTime() + RETRY_DELAY_TICKS;
        return super.moveTo(target.getX(), target.getY(), target.getZ(), speed);
    }

    private void cancelPending() {
        if (pending != null) {
            pending.search.cancel(false);
            pending = null;
        }
    }

    private static Node node(BlockPos cell) {
        Node node = new Node(cell.getX(), cell.getY(), cell.getZ());
        node.type = BlockPathTypes.WATER;
        return node;
    }

    private static final class Pending {
        private final BlockPos start;
        private final BlockPos target;
        private final long deadline;
        private final CompletableFuture<PathResult> search;
        private double speed;

        private Pending(BlockPos start, BlockPos target, double speed, long deadline, CompletableFuture<PathResult> search) {
            this.start = start;
            this.target = target;
            this.speed = speed;
            this.deadline = deadline;
            this.search = search;
        }
    }

    private static final class SwimPath extends Path {
        private final BlockPos min;
        private final BlockPos max;

        private SwimPath(List<Node> nodes, BlockPos target, BlockPos start) {
            super(nodes, target, true);
            int minX = start.getX();
            int minY = start.getY();
            int minZ = start.getZ();
            int maxX = minX;
            int maxY = minY;
            int maxZ = minZ;
            for (Node node : nodes) {
                minX = Math.min(minX, node.x);
                minY = Math.min(minY, node.y);
                minZ = Math.min(minZ, node.z);
                maxX = Math.max(maxX, node.x);
                maxY = Math.max(maxY, node.y);
                maxZ = Math.max(maxZ, node.z);
            }
            min = new BlockPos(minX - 1, minY - 1, minZ - 1);
            max = new BlockPos(maxX + 1, maxY + 1, maxZ + 1);
        }

        private boolean crosses(BlockPos pos) {
            return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                    && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                    && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        }
    }
}
