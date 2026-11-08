package com.y271727uy.FRMC.integration.delighto_flight;

import com.y271727uy.computatio.algorithm.fastpath.model.GridPos;
import com.y271727uy.computatio.algorithm.fastpath.model.PathCapabilities;
import com.y271727uy.computatio.algorithm.fastpath.model.PathIntent;
import com.y271727uy.computatio.algorithm.fastpath.model.PathRequest;
import com.y271727uy.computatio.algorithm.fastpath.model.PathResult;
import com.y271727uy.computatio.algorithm.fastpath.model.PathVector;
import com.y271727uy.computatio.algorithm.fastpath.model.PathWaypoint;
import com.y271727uy.computatio.algorithm.fastpath.model.TraversalSnapshot;
import com.y271727uy.computatio.api.fastpath.FastPath;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Converts a small dry flight route to the format used by the vanilla navigation follower. */
public final class AllayChefPathfinding {
    private static final int MAX_DISTANCE = 20;
    private static final int MAX_SNAPSHOT_CELLS = 2048;
    private static final int DETOUR_MARGIN = 2;
    private static final PathCapabilities FLIGHT = new PathCapabilities(false, true, false, false);

    private AllayChefPathfinding() {
    }

    public static Path find(Allay allay, BlockPos target, int closeEnough, float followRange) {
        BlockPos start = allay.blockPosition();
        if (!(allay.level() instanceof ServerLevel level) || !level.getServer().isSameThread()
                || level.isDebug() || start.distManhattan(target) > MAX_DISTANCE
                || start.distSqr(target) >= followRange * followRange) {
            return null;
        }

        Vec3 origin = allay.position();
        Vec3 destination = Vec3.atBottomCenterOf(target);
        AABB body = allay.getBoundingBox().move(origin.scale(-1));
        AABB direct = sweptBounds(body, origin, destination);
        if (isLoaded(level, direct) && clear(level, allay, direct)) {
            List<Node> nodes = new ArrayList<>(1);
            nodes.add(new Node(target.getX(), target.getY(), target.getZ()));
            return new ChefFlightPath(nodes, target, closeEnough, direct);
        }

        BlockPos min = new BlockPos(Math.min(start.getX(), target.getX()) - DETOUR_MARGIN,
                Math.max(level.getMinBuildHeight(), Math.min(start.getY(), target.getY()) - DETOUR_MARGIN),
                Math.min(start.getZ(), target.getZ()) - DETOUR_MARGIN);
        BlockPos max = new BlockPos(Math.max(start.getX(), target.getX()) + DETOUR_MARGIN,
                Math.min(level.getMaxBuildHeight() - 1, Math.max(start.getY(), target.getY()) + DETOUR_MARGIN),
                Math.max(start.getZ(), target.getZ()) + DETOUR_MARGIN);
        int volume = (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1)
                * (max.getZ() - min.getZ() + 1);
        AABB region = new AABB(min, max.offset(1, 1, 1));
        if (min.getY() > max.getY() || volume > MAX_SNAPSHOT_CELLS || !isLoaded(level, region)
                || !level.getBlockState(start).isAir()) {
            return null;
        }

        String world = level.dimension().location().toString();
        PathRequest request = new PathRequest(world, grid(start),
                new PathVector(destination.x, destination.y, destination.z), PathIntent.WALK, FLIGHT);
        TraversalSnapshot.Builder snapshot = TraversalSnapshot.builder(world, request.start(), request.targetBlock(),
                min.getY(), max.getY(), level.getGameTime(), true);
        if (!scanAir(level, min, max, snapshot)) {
            return null;
        }
        PathResult result = FastPath.find(request, snapshot.build());
        if (!result.success() || result.waypoints().isEmpty()) {
            return null;
        }

        // The snapshot uses air cells as candidates. Verify the actual body against live collision shapes,
        // including protruding shapes such as fences and any segments merged by the search.
        List<Node> nodes = new ArrayList<>(result.waypoints().size());
        Vec3 previous = origin;
        BlockPos end = start;
        AABB corridor = body.move(origin);
        for (PathWaypoint waypoint : result.waypoints()) {
            GridPos pos = waypoint.blockPosition();
            end = new BlockPos(pos.x(), pos.y(), pos.z());
            Vec3 next = Vec3.atBottomCenterOf(end);
            AABB segment = sweptBounds(body, previous, next);
            if (!clear(level, allay, segment)) {
                return null;
            }
            nodes.add(new Node(pos.x(), pos.y(), pos.z()));
            corridor = corridor.minmax(segment);
            previous = next;
        }
        return end.distManhattan(target) <= closeEnough ? new ChefFlightPath(nodes, target, closeEnough, corridor) : null;
    }

    private static GridPos grid(BlockPos pos) {
        return new GridPos(pos.getX(), pos.getY(), pos.getZ());
    }

    private static AABB sweptBounds(AABB body, Vec3 from, Vec3 to) {
        return body.move(from).expandTowards(to.subtract(from));
    }

    private static boolean isLoaded(ServerLevel level, AABB bounds) {
        BlockPos min = BlockPos.containing(bounds.minX - 1, bounds.minY, bounds.minZ - 1);
        BlockPos max = BlockPos.containing(bounds.maxX + 1, bounds.maxY, bounds.maxZ + 1);
        return bounds.minY >= level.getMinBuildHeight() && bounds.maxY <= level.getMaxBuildHeight()
                && level.getWorldBorder().isWithinBounds(min) && level.getWorldBorder().isWithinBounds(max)
                && level.hasChunksAt(min, max);
    }

    private static boolean clear(ServerLevel level, Allay allay, AABB bounds) {
        AABB inside = bounds.deflate(1.0E-7);
        return scanAir(level, BlockPos.containing(inside.minX, inside.minY, inside.minZ),
                BlockPos.containing(inside.maxX, inside.maxY, inside.maxZ), null)
                && !level.getBlockCollisions(allay, bounds).iterator().hasNext();
    }

    /** Collects air cells, or checks that every cell is air when no snapshot is supplied. */
    static boolean scanAir(ServerLevel level, BlockPos min, BlockPos max, TraversalSnapshot.Builder snapshot) {
        if (level.isOutsideBuildHeight(min) || level.isOutsideBuildHeight(max)) {
            return false;
        }
        int maxSectionX = SectionPos.blockToSectionCoord(max.getX());
        int minSectionY = SectionPos.blockToSectionCoord(min.getY());
        int maxSectionY = SectionPos.blockToSectionCoord(max.getY());
        int minSectionZ = SectionPos.blockToSectionCoord(min.getZ());
        int maxSectionZ = SectionPos.blockToSectionCoord(max.getZ());
        for (int sx = SectionPos.blockToSectionCoord(min.getX()); sx <= maxSectionX; sx++) {
            int baseX = SectionPos.sectionToBlockCoord(sx);
            int minX = Math.max(min.getX(), baseX);
            int maxX = Math.min(max.getX(), baseX + 15);
            for (int sz = minSectionZ; sz <= maxSectionZ; sz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(sx, sz);
                if (chunk == null) {
                    return false;
                }
                int baseZ = SectionPos.sectionToBlockCoord(sz);
                int minZ = Math.max(min.getZ(), baseZ);
                int maxZ = Math.min(max.getZ(), baseZ + 15);
                for (int sy = minSectionY; sy <= maxSectionY; sy++) {
                    LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
                    if (!scanSection(section, minX, maxX, minZ, maxZ, sy, min.getY(), max.getY(), snapshot)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean scanSection(LevelChunkSection section, int minX, int maxX, int minZ, int maxZ,
                                       int sectionY, int minY, int maxY, TraversalSnapshot.Builder snapshot) {
        boolean empty = section.hasOnlyAir();
        if (empty && snapshot == null) {
            return true;
        }
        int baseY = SectionPos.sectionToBlockCoord(sectionY);
        for (int y = Math.max(minY, baseY); y <= Math.min(maxY, baseY + 15); y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    if (empty || section.getBlockState(x & 15, y & 15, z & 15).isAir()) {
                        if (snapshot != null) {
                            snapshot.addBodyPassage(x, y, z);
                        }
                    } else if (snapshot == null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
