package com.y271727uy.FRMC.integration.manors_bounty;

import net.mcreator.manors_bounty.init.ManorsBountyModBlocks;
import net.mcreator.manors_bounty.network.ManorsBountyModVariables.MapVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class VineGrowth {
    private static final String STAGE = "blockstate";
    private static final String AGE = "age";
    private static final Direction[] ARBOR_SPREAD_ORDER = {
            Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
    };

    private VineGrowth() {
    }

    public static void growStake(LevelAccessor world, BlockPos pos, BlockState tickState) {
        BlockState current = world.getBlockState(pos);
        int stage = getInt(current, STAGE);
        if (stage != 1 && stage != 3 && stage != 5) {
            return;
        }
        if (!(Math.random() < 0.33D) || getInt(tickState, AGE) >= 7) {
            return;
        }

        BlockPos above = pos.above();
        BlockState aboveState = world.getBlockState(above);
        if (aboveState.is(ManorsBountyModBlocks.VINE_STAKE.get()) && getInt(aboveState, STAGE) == 0) {
            setInt(world, pos, current, STAGE, stage + 1);
            setInt(world, above, world.getBlockState(above), STAGE, stage);
            int nextAge = getInt(world.getBlockState(pos), AGE) + 1;
            setInt(world, above, world.getBlockState(above), AGE, nextAge);
            aboveState = world.getBlockState(above);
        }
        if (aboveState.is(ManorsBountyModBlocks.VINE_ARBOR.get()) && getInt(aboveState, STAGE) == 0) {
            setInt(world, above, aboveState, STAGE, (stage + 1) / 2);
            setInt(world, pos, world.getBlockState(pos), STAGE, stage + 1);
        }
    }

    public static void growArbor(LevelAccessor world, BlockPos pos, BlockState tickState) {
        int currentStage = getInt(world.getBlockState(pos), STAGE);
        // Upstream checks each stage in order. Writes may synchronously notify neighbors.
        for (int stage = 1; stage <= 3; stage++) {
            if (currentStage != stage) {
                continue;
            }
            if (Math.random() < 0.33D && getInt(tickState, AGE) < 2) {
                spreadArbor(world, pos, stage);
            }
            if (Math.random() < 0.35D) {
                growArborFruit(world, pos.below(), stage);
            }
            if (stage < 3) {
                currentStage = getInt(world.getBlockState(pos), STAGE);
            }
        }
    }

    private static void spreadArbor(LevelAccessor world, BlockPos pos, int stage) {
        Block arbor = ManorsBountyModBlocks.VINE_ARBOR.get();
        for (Direction direction : ARBOR_SPREAD_ORDER) {
            BlockPos neighbor = pos.relative(direction);
            BlockState state = world.getBlockState(neighbor);
            if (state.is(arbor) && getInt(state, STAGE) == 0) {
                setInt(world, neighbor, state, STAGE, stage);
                int nextAge = getInt(world.getBlockState(pos), AGE) + 1;
                setInt(world, neighbor, world.getBlockState(neighbor), AGE, nextAge);
            }
        }
    }

    private static void growArborFruit(LevelAccessor world, BlockPos pos, int arborStage) {
        Block crop = switch (arborStage) {
            case 1 -> ManorsBountyModBlocks.GRAPE_CROP.get();
            case 2 -> ManorsBountyModBlocks.PASSION_FRUIT_CROP.get();
            case 3 -> ManorsBountyModBlocks.CUCUMBER_CROP.get();
            default -> throw new IllegalArgumentException("Unexpected vine arbor stage " + arborStage);
        };
        BlockState state = world.getBlockState(pos);
        if (state.canBeReplaced()) {
            world.setBlock(pos, crop.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        if (!state.is(crop)) {
            return;
        }

        int cropStage = getInt(state, STAGE);
        if (cropStage == 0 || cropStage == 1) {
            int nextStage = cropStage + 1;
            if (arborStage == 1 && cropStage == 1 && !(Math.random() > MapVariables.get(world).rare_variant_chance)) {
                nextStage = 3;
            }
            setInt(world, pos, state, STAGE, nextStage);
            setInt(world, pos, world.getBlockState(pos), AGE, cropStage + 1);
        }
    }

    private static int getInt(BlockState state, String name) {
        if (state.getBlock().getStateDefinition().getProperty(name) instanceof IntegerProperty property) {
            return state.getValue(property);
        }
        return -1;
    }

    private static void setInt(LevelAccessor world, BlockPos pos, BlockState state, String name, int value) {
        if (state.getBlock().getStateDefinition().getProperty(name) instanceof IntegerProperty property
                && state.getValue(property) != value && property.getPossibleValues().contains(value)) {
            world.setBlock(pos, state.setValue(property, value), Block.UPDATE_ALL);
        }
    }
}
