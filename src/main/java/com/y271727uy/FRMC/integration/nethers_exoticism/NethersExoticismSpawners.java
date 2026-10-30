package com.y271727uy.FRMC.integration.nethers_exoticism;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

public final class NethersExoticismSpawners {
    private NethersExoticismSpawners() {
    }

    public static void grow(LevelAccessor world, BlockPos origin, Block spawner, Block replacement, int negativeRadius, int positiveRadius, int headroom, Grower grower) {
        NethersExoticismSupport.replaceNearbySpawners(world, origin, spawner, replacement, negativeRadius, positiveRadius);
        if (hasHeadroom(world, origin, headroom)) {
            grower.grow(world, origin.above());
        }
        world.setBlock(origin, replacement.defaultBlockState(), 3);
    }

    public static boolean hasHeadroom(LevelAccessor world, BlockPos origin, int blocks) {
        for (int height = 1; height <= blocks; height++) {
            if (!world.isEmptyBlock(origin.above(height))) {
                return false;
            }
        }
        return true;
    }

    @FunctionalInterface
    public interface Grower {
        void grow(LevelAccessor world, BlockPos origin);
    }
}
