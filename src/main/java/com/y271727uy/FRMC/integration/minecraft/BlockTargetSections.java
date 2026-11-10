package com.y271727uy.FRMC.integration.minecraft;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** A conservative section filter for a single MoveToBlockGoal search. */
public final class BlockTargetSections {
    private final int minX;
    private final int minY;
    private final int minZ;
    private final boolean[][][] candidates;
    private boolean anyCandidates;

    private BlockTargetSections(int minX, int minY, int minZ, int width, int height, int depth) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        candidates = new boolean[width][height][depth];
    }

    public static BlockTargetSections capture(ServerLevel level, BlockPos origin, int searchRange,
            int verticalStart, int verticalRange, Predicate<BlockState> candidate) {
        if (!level.getServer().isSameThread() || searchRange < 1 || searchRange > 32
                || verticalStart > verticalRange || verticalStart < -16 || verticalRange < 0 || verticalRange > 32) {
            return null;
        }
        int extent = Math.max(Math.abs(verticalStart), verticalRange);
        int lowY = origin.getY() - extent - 1;
        int highY = origin.getY() + extent - 1;
        if (level.isOutsideBuildHeight(lowY) || level.isOutsideBuildHeight(highY)) {
            return null;
        }
        int radius = searchRange - 1;
        int minX = (origin.getX() - radius) >> 4;
        int maxX = (origin.getX() + radius) >> 4;
        int minY = lowY >> 4;
        int maxY = highY >> 4;
        int minZ = (origin.getZ() - radius) >> 4;
        int maxZ = (origin.getZ() + radius) >> 4;
        var result = new BlockTargetSections(minX, minY, minZ,
                maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                var chunk = level.getChunkSource().getChunkNow(x, z);
                for (int y = minY; y <= maxY; y++) {
                    // Leave unloaded chunks to the original search without loading them here.
                    if (chunk == null || chunk.getSection(level.getSectionIndexFromSectionY(y)).maybeHas(candidate)) {
                        result.candidates[x - minX][y - minY][z - minZ] = true;
                        result.anyCandidates = true;
                    }
                }
            }
        }
        return result;
    }

    public boolean hasCandidates() {
        return anyCandidates;
    }

    public boolean mayContain(BlockPos pos) {
        int x = (pos.getX() >> 4) - minX;
        int y = (pos.getY() >> 4) - minY;
        int z = (pos.getZ() >> 4) - minZ;
        // A modified search outside the sampled area must still run the original predicate.
        return x < 0 || x >= candidates.length || y < 0 || y >= candidates[x].length
                || z < 0 || z >= candidates[x][y].length || candidates[x][y][z];
    }
}
