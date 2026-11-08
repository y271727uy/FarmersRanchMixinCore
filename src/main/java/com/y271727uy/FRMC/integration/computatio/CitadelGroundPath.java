package com.y271727uy.FRMC.integration.computatio;

import com.github.alexthe666.citadel.server.entity.pathfinding.raycoms.PathPointExtended;
import com.mojang.logging.LogUtils;
import com.y271727uy.computatio.algorithm.fastpath.model.GridPos;
import com.y271727uy.computatio.algorithm.fastpath.model.MovementMode;
import com.y271727uy.computatio.algorithm.fastpath.model.PathCapabilities;
import com.y271727uy.computatio.algorithm.fastpath.model.PathIntent;
import com.y271727uy.computatio.algorithm.fastpath.model.PathRequest;
import com.y271727uy.computatio.algorithm.fastpath.model.PathResult;
import com.y271727uy.computatio.algorithm.fastpath.model.PathResult.Status;
import com.y271727uy.computatio.algorithm.fastpath.model.PathVector;
import com.y271727uy.computatio.algorithm.fastpath.model.PathWaypoint;
import com.y271727uy.computatio.algorithm.fastpath.model.TraversalCell;
import com.y271727uy.computatio.algorithm.fastpath.model.TraversalSnapshot;
import com.y271727uy.computatio.api.fastpath.FastPath;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class CitadelGroundPath {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_DISTANCE = 12;
    private static final int DETOUR_MARGIN = 2;
    private static final int MAX_AGE_TICKS = 20;
    private static final PathCapabilities FLAT_GROUND = new PathCapabilities(false, false, false, false);

    private final BlockPos start;
    private final BlockPos target;
    private final PathRequest request;
    private final TraversalSnapshot snapshot;

    private CitadelGroundPath(BlockPos start, BlockPos target, PathRequest request, TraversalSnapshot snapshot) {
        this.start = start;
        this.target = target;
        this.request = request;
        this.snapshot = snapshot;
    }

    public BlockPos target() {
        return target;
    }

    public static CitadelGroundPath capture(Mob mob, BlockPos start, BlockPos target, int range) {
        if (!(mob.level() instanceof ServerLevel level) || !level.getServer().isSameThread()
                || !mob.onGround() || mob.isInWaterOrBubble() || mob.onClimbable()
                || !start.equals(mob.blockPosition()) || start.getY() != target.getY()
                || start.equals(target) || start.distManhattan(target) > Math.min(range, MAX_DISTANCE)) {
            return null;
        }
        int y = start.getY();
        BlockPos min = new BlockPos(Math.min(start.getX(), target.getX()) - DETOUR_MARGIN, y,
                Math.min(start.getZ(), target.getZ()) - DETOUR_MARGIN);
        BlockPos max = new BlockPos(Math.max(start.getX(), target.getX()) + DETOUR_MARGIN, y,
                Math.max(start.getZ(), target.getZ()) + DETOUR_MARGIN);
        Vec3 position = mob.position();
        AABB body = mob.getBoundingBox().move(position.scale(-1));
        int padding = Mth.ceil(mob.getBbWidth() / 2.0);
        int height = Mth.ceil(body.maxY);
        if (!isAvailable(level, min.offset(-padding, 0, -padding), max.offset(padding, height, padding))
                || !isFlatCell(level, start) || !isFlatCell(level, target)) {
            return null;
        }

        String world = level.dimension().location().toString();
        Vec3 end = Vec3.atBottomCenterOf(target);
        PathRequest request = new PathRequest(world, grid(start), new PathVector(end.x, end.y, end.z),
                PathIntent.WALK, FLAT_GROUND);
        TraversalSnapshot.Builder snapshot = TraversalSnapshot.builder(world, request.start(),
                request.targetBlock(), y, y, level.getGameTime(), true);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (isFlatCell(level, pos)
                    && !level.getBlockCollisions(mob, body.move(Vec3.atBottomCenterOf(pos))).iterator().hasNext()) {
                snapshot.addCell(new TraversalCell(pos.getX(), y, pos.getZ(), y, false, false, false, 1.0));
                snapshot.addBodyPassage(pos.getX(), y, pos.getZ());
            }
        }
        TraversalSnapshot terrain = snapshot.build();
        return terrain.cell(request.start()) != null && terrain.cell(request.targetBlock()) != null
                ? new CitadelGroundPath(start, target, request, terrain) : null;
    }

    public boolean canApply(Mob mob, Path path) {
        if (!(mob.level() instanceof ServerLevel level) || level.getGameTime() - snapshot.capturedAt() > MAX_AGE_TICKS
                || !start.equals(mob.blockPosition()) || !mob.onGround()
                || path == null || path.getNodeCount() == 0) {
            return false;
        }
        Vec3 previous = mob.position();
        AABB body = mob.getBoundingBox().move(previous.scale(-1));
        for (int index = 0; index < path.getNodeCount(); index++) {
            Node node = path.getNode(index);
            Vec3 next = Vec3.atBottomCenterOf(new BlockPos(node.x, node.y, node.z));
            AABB sweep = body.move(previous).expandTowards(next.subtract(previous));
            BlockPos min = BlockPos.containing(sweep.minX + 1.0E-7, start.getY(), sweep.minZ + 1.0E-7);
            BlockPos max = BlockPos.containing(sweep.maxX - 1.0E-7, start.getY(), sweep.maxZ - 1.0E-7);
            if (!isAvailable(level, min, max.above(Mth.ceil(body.maxY)))
                    || level.getBlockCollisions(mob, sweep).iterator().hasNext()) {
                return false;
            }
            for (BlockPos cell : BlockPos.betweenClosed(min, max)) {
                if (!isFlatCell(level, cell)) {
                    return false;
                }
            }
            previous = next;
        }
        return true;
    }

    public Path find() {
        PathResult route;
        try {
            route = FastPath.find(request, snapshot);
        } catch (RuntimeException exception) {
            LOGGER.warn("Computatio could not search a Citadel ground route", exception);
            return null;
        }
        if (route.status() != Status.EXACT_TARGET) {
            return null;
        }
        List<Node> nodes = new ArrayList<>(route.waypoints().size());
        for (PathWaypoint waypoint : route.waypoints()) {
            GridPos cell = waypoint.blockPosition();
            if (waypoint.mode() != MovementMode.WALK || cell.y() != target.getY()) {
                return null;
            }
            nodes.add(new PathPointExtended(new BlockPos(cell.x(), cell.y(), cell.z())));
        }
        return nodes.isEmpty() ? null : new Path(nodes, target, true);
    }

    private static boolean isAvailable(ServerLevel level, BlockPos min, BlockPos max) {
        BlockPos low = min.offset(-1, -1, -1);
        BlockPos high = max.offset(1, 1, 1);
        return !level.isOutsideBuildHeight(low) && !level.isOutsideBuildHeight(high)
                && level.getWorldBorder().isWithinBounds(low) && level.getWorldBorder().isWithinBounds(high)
                && level.hasChunksAt(low, high);
    }

    private static boolean isFlatCell(ServerLevel level, BlockPos pos) {
        BlockPos support = pos.below();
        return level.getBlockState(pos).isAir()
                && level.getBlockState(support).isCollisionShapeFullBlock(level, support)
                && WalkNodeEvaluator.getBlockPathTypeStatic(level, pos.mutable()) == BlockPathTypes.WALKABLE;
    }

    private static GridPos grid(BlockPos pos) {
        return new GridPos(pos.getX(), pos.getY(), pos.getZ());
    }
}
