package com.y271727uy.FRMC.integration.computatio;

import com.y271727uy.computatio.algorithm.fastpath.model.GridPos;
import com.y271727uy.computatio.algorithm.fastpath.model.PathCapabilities;
import com.y271727uy.computatio.algorithm.fastpath.model.PathIntent;
import com.y271727uy.computatio.algorithm.fastpath.model.PathRequest;
import com.y271727uy.computatio.algorithm.fastpath.model.PathResult;
import com.y271727uy.computatio.algorithm.fastpath.model.PathVector;
import com.y271727uy.computatio.algorithm.fastpath.model.PathWaypoint;
import com.y271727uy.computatio.algorithm.fastpath.model.TraversalCell;
import com.y271727uy.computatio.algorithm.fastpath.model.TraversalSnapshot;
import com.y271727uy.computatio.api.fastpath.FastPath;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/** Bounded ground search for the short approach to a selected piece of furniture. */
public final class CatFurniturePath {
    private static final int MAX_DISTANCE = 12;
    private static final int MAX_HEIGHT_DIFFERENCE = 2;
    private static final int DETOUR_MARGIN = 1;
    private static final PathCapabilities GROUND = new PathCapabilities(false, false, false, false);

    private CatFurniturePath() {
    }

    public static Path find(Cat cat, GroundPathNavigation navigation, BlockPos target) {
        BlockPos origin = cat.blockPosition();
        double range = cat.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (!(cat.level() instanceof ServerLevel level) || !level.getServer().isSameThread()
                || !(navigation.getNodeEvaluator() instanceof WalkNodeEvaluator evaluator)
                || !cat.onGround() || cat.isInWaterOrBubble() || cat.onClimbable() || cat.isPassenger()
                || cat.getBbWidth() >= 1.0F || cat.getBbHeight() >= 1.0F
                || origin.distManhattan(target) > MAX_DISTANCE || origin.distSqr(target) >= range * range
                || Math.abs(origin.getY() - target.getY()) > MAX_HEIGHT_DIFFERENCE) {
            return null;
        }

        BlockPos min = new BlockPos(Math.min(origin.getX(), target.getX()) - DETOUR_MARGIN,
                Math.min(origin.getY(), target.getY()) - 1, Math.min(origin.getZ(), target.getZ()) - DETOUR_MARGIN);
        BlockPos max = new BlockPos(Math.max(origin.getX(), target.getX()) + DETOUR_MARGIN,
                Math.max(origin.getY(), target.getY()), Math.max(origin.getZ(), target.getZ()) + DETOUR_MARGIN);
        BlockPos regionMin = min.offset(-2, -2, -2);
        BlockPos regionMax = max.offset(2, 2, 2);
        if (level.isOutsideBuildHeight(regionMin) || level.isOutsideBuildHeight(regionMax)
                || !level.getWorldBorder().isWithinBounds(regionMin) || !level.getWorldBorder().isWithinBounds(regionMax)
                || !level.hasChunksAt(regionMin, regionMax)) {
            return null;
        }

        PathNavigationRegion region = new PathNavigationRegion(level, regionMin, regionMax);
        evaluator.prepare(region, cat);
        try {
            Node start = evaluator.getStart();
            if (start == null || start.y < min.getY() || start.y > max.getY()) {
                return null;
            }
            String world = level.dimension().location().toString();
            PathRequest request = new PathRequest(world, new GridPos(start.x, start.y, start.z),
                    new PathVector(target.getX() + 0.5, target.getY(), target.getZ() + 0.5), PathIntent.WALK, GROUND);
            TraversalSnapshot.Builder snapshot = TraversalSnapshot.builder(world, request.start(),
                    request.targetBlock(), min.getY(), max.getY(), level.getGameTime(), true);
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();
                BlockPathTypes type = evaluator.getBlockPathType(region, x, y, z, cat);
                if (type == BlockPathTypes.WALKABLE && cat.getPathfindingMalus(type) == 0.0F) {
                    snapshot.addCell(new TraversalCell(x, y, z, WalkNodeEvaluator.getFloorLevel(region, pos),
                            false, false, false, 1.0));
                } else if (type != BlockPathTypes.OPEN) {
                    continue;
                }
                snapshot.addBodyPassage(x, y, z);
            }
            TraversalSnapshot terrain = snapshot.build();
            if (terrain.cell(request.start()) == null || terrain.cell(request.targetBlock()) == null) {
                return null;
            }
            PathResult result = FastPath.find(request, terrain);
            return result.status() == PathResult.Status.EXACT_TARGET
                    ? toVanillaPath(evaluator, start, target, result.waypoints()) : null;
        } finally {
            evaluator.done();
        }
    }

    private static Path toVanillaPath(WalkNodeEvaluator evaluator, Node start, BlockPos target,
                                      List<PathWaypoint> waypoints) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(start);
        Node previous = start;
        Node[] neighbors = new Node[32];
        for (PathWaypoint waypoint : waypoints) {
            GridPos end = waypoint.blockPosition();
            int dx = end.x() - previous.x;
            int dz = end.z() - previous.z;
            int steps = Math.max(Math.abs(dx), Math.abs(dz));
            if (steps == 0 && end.y() == previous.y) {
                continue;
            }
            if (end.y() != previous.y) {
                // Height changes must correspond to an actual vanilla jump or drop edge.
                if (steps > 1) {
                    return null;
                }
                steps = 1;
            }
            int x = previous.x;
            int z = previous.z;
            // Expand smoothed segments so vanilla follows only verified neighboring nodes.
            for (int step = 1; step <= steps; step++) {
                int nextX = x + Math.round((float) dx * step / steps);
                int nextZ = z + Math.round((float) dz * step / steps);
                previous = neighbor(evaluator, neighbors, previous, nextX, end.y(), nextZ);
                if (previous == null) {
                    return null;
                }
                nodes.add(previous);
            }
        }
        return nodes.size() > 1 && target.equals(previous.asBlockPos()) ? new Path(nodes, target, true) : null;
    }

    private static Node neighbor(WalkNodeEvaluator evaluator, Node[] neighbors, Node from, int x, int y, int z) {
        int count = evaluator.getNeighbors(neighbors, from);
        for (int index = 0; index < count; index++) {
            Node next = neighbors[index];
            if (next.x == x && next.y == y && next.z == z && next.type == BlockPathTypes.WALKABLE) {
                return next;
            }
        }
        return null;
    }
}
