package com.y271727uy.FRMC.integration.oceanic_delight;

import net.mcreator.oceanicdelight.init.OceanicDelightModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

/**
 * Direct replacements for Oceanic Delight's generated sea-grape procedures.
 * The original growth path parses and executes a {@code /setblock} command.
 */
public final class OceanicDelightSupport {
    private OceanicDelightSupport() {
    }

    public static boolean canGrowAbove(LevelAccessor world, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = world.getBlockState(above);
        Block aboveBlock = aboveState.getBlock();
        if (aboveBlock == plant() || aboveBlock == fruit()) {
            return false;
        }
        return aboveState.getFluidState().isSource() && aboveState.getFluidState().getType() == Fluids.WATER;
    }

    public static void growAbove(LevelAccessor world, BlockPos pos) {
        BlockPos above = pos.above();
        BlockPos below = pos.below();
        BlockPos twoBelow = below.below();
        boolean fruit = world.getBlockState(below).is(plant()) && world.getBlockState(twoBelow).is(plant());
        world.setBlock(above, fruit ? waterloggedFruit() : waterloggedPlant(), Block.UPDATE_ALL);
    }

    private static Block plant() {
        return OceanicDelightModBlocks.SEA_GRAPE_PLANT.get();
    }

    private static Block fruit() {
        return OceanicDelightModBlocks.SEA_GRAPE_FRUIT.get();
    }

    private static BlockState waterloggedPlant() {
        return plant().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
    }

    private static BlockState waterloggedFruit() {
        return fruit().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
    }
}
