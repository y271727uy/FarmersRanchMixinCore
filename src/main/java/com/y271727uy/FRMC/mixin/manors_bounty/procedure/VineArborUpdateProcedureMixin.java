package com.y271727uy.FRMC.mixin.manors_bounty.procedure;

import net.mcreator.manors_bounty.init.ManorsBountyModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.mcreator.manors_bounty.procedures.VineArborUpdateProcedure")
public abstract class VineArborUpdateProcedureMixin {
    @Unique
    private static final String FRMC$AGE_PROPERTY = "age";
    @Unique
    private static final String FRMC$BLOCKSTATE_PROPERTY = "blockstate";
    @Unique
    private static final int FRMC$RESET_VALUE = 0;

    /**
     * @author FRMC
     * @reason Replace the generated vine arbor update procedure with a compatibility-safe implementation that caches states and avoids redundant block updates.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = world.getBlockState(pos);
        int age = frmc$getIntProperty(state, FRMC$AGE_PROPERTY);
        if (age < 0) {
            return;
        }

        if (age == 0) {
            BlockState belowState = world.getBlockState(pos.below());
            int belowBlockstate = frmc$getIntProperty(belowState, FRMC$BLOCKSTATE_PROPERTY);
            if (belowState.is(ManorsBountyModBlocks.VINE_STAKE.get()) && frmc$isSupportedStakeStage(belowBlockstate)) {
                return;
            }

            frmc$resetState(world, pos, state);
            return;
        }

        if (age == 1) {
            frmc$resetIfHorizontalChainInvalid(world, pos, state, 0);
            return;
        }

        if (age == 2) {
            frmc$resetIfHorizontalChainInvalid(world, pos, state, 1);
        }
    }

    @Unique
    private static void frmc$resetIfHorizontalChainInvalid(LevelAccessor world, BlockPos pos, BlockState state, int requiredNeighborAge) {
        BlockState[] horizontalStates = new BlockState[4];
        int index = 0;
        boolean hasRequiredNeighborAge = false;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighborState = world.getBlockState(pos.relative(direction));
            horizontalStates[index++] = neighborState;
            if (!hasRequiredNeighborAge && frmc$getIntProperty(neighborState, FRMC$AGE_PROPERTY) == requiredNeighborAge) {
                hasRequiredNeighborAge = true;
            }
        }

        if (!hasRequiredNeighborAge) {
            return;
        }

        for (BlockState neighborState : horizontalStates) {
            if (frmc$getIntProperty(neighborState, FRMC$BLOCKSTATE_PROPERTY) > 0) {
                return;
            }
        }

        frmc$resetState(world, pos, state);
    }

    @Unique
    private static boolean frmc$isSupportedStakeStage(int blockstate) {
        return blockstate == 2 || blockstate == 4 || blockstate == 6;
    }

    @Unique
    private static int frmc$getIntProperty(BlockState state, String propertyName) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(propertyName);
        if (!(property instanceof IntegerProperty integerProperty)) {
            return -1;
        }
        return state.getValue(integerProperty);
    }

    @Unique
    private static void frmc$resetState(LevelAccessor world, BlockPos pos, BlockState state) {
        BlockState updatedState = frmc$setIntProperty(state, FRMC$BLOCKSTATE_PROPERTY, FRMC$RESET_VALUE);
        updatedState = frmc$setIntProperty(updatedState, FRMC$AGE_PROPERTY, FRMC$RESET_VALUE);
        if (updatedState != state) {
            world.setBlock(pos, updatedState, 3);
        }
    }

    @Unique
    private static BlockState frmc$setIntProperty(BlockState state, String propertyName, int value) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(propertyName);
        if (!(property instanceof IntegerProperty integerProperty)) {
            return state;
        }
        if (!integerProperty.getPossibleValues().contains(value)) {
            return state;
        }
        return state.setValue(integerProperty, value);
    }
}

