package com.y271727uy.FRMC.integration.alexsmobs;

import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class CrowFearSearch {
    private CrowFearSearch() {
    }

    public static BlockPos find(ServerLevel level, BlockPos origin, int radius) {
        int minY = Math.max(level.getMinBuildHeight(), origin.getY() - 9);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, origin.getY() + 1);
        if (minY > maxY) {
            return null;
        }
        int minXSection = (origin.getX() - radius) >> 4;
        int minYSection = minY >> 4;
        int minZSection = (origin.getZ() - radius) >> 4;
        int width = ((origin.getX() + radius) >> 4) - minXSection + 1;
        int depth = ((origin.getZ() + radius) >> 4) - minZSection + 1;
        int height = (maxY >> 4) - minYSection + 1;
        LevelChunkSection[][][] candidates = new LevelChunkSection[width][depth][height];
        boolean hasCandidate = false;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                var chunk = level.getChunkSource().getChunkNow(minXSection + x, minZSection + z);
                if (chunk == null) {
                    continue;
                }
                for (int y = 0; y < height; y++) {
                    var section = chunk.getSection(level.getSectionIndexFromSectionY(minYSection + y));
                    if (section.maybeHas(state -> state.is(AMTagRegistry.CROW_FEARS))) {
                        candidates[x][z][y] = section;
                        hasCandidate = true;
                    }
                }
            }
        }
        if (!hasCandidate) {
            return null;
        }

        // Preserve the goal's bottom-to-top, outward ring order and its 0, 1, -1, ... tie break.
        for (int y = minY; y <= maxY; y++) {
            int sectionY = (y >> 4) - minYSection;
            for (int ring = 0; ring <= radius; ring++) {
                for (int dx = 0; dx <= ring; dx = nextOffset(dx)) {
                    int x = origin.getX() + dx;
                    var column = candidates[(x >> 4) - minXSection];
                    int firstZ = Math.abs(dx) < ring ? ring : 0;
                    for (int dz = firstZ; dz <= ring; dz = nextOffset(dz)) {
                        int z = origin.getZ() + dz;
                        var section = column[(z >> 4) - minZSection][sectionY];
                        if (section != null && section.getBlockState(x & 15, y & 15, z & 15).is(AMTagRegistry.CROW_FEARS)) {
                            return new BlockPos(x, y, z);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static int nextOffset(int offset) {
        return offset > 0 ? -offset : 1 - offset;
    }
}
