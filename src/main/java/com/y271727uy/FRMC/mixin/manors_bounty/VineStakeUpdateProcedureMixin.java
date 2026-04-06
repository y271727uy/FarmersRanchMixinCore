package com.y271727uy.FRMC.mixin.manors_bounty;

import net.mcreator.manors_bounty.init.ManorsBountyModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.mcreator.manors_bounty.procedures.VineStakeUpdateProcedure")
public abstract class VineStakeUpdateProcedureMixin {
    @Unique
    private static final String FRMC$AGE_PROPERTY = "age";
    @Unique
    private static final String FRMC$BLOCKSTATE_PROPERTY = "blockstate";
    @Unique
    private static final int FRMC$RESET_VALUE = 0;

    /**
     * @author FRMC
     * @reason Replace the generated vine stake update procedure with a compatibility-safe implementation that preserves stage behavior while reducing repeated block lookups.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = world.getBlockState(pos);

        BlockPos belowPos = pos.below();
        BlockState belowState = world.getBlockState(belowPos);
        if (!frmc$hasValidSupportBelow(belowState) && !belowState.is(BlockTags.DIRT)) {
            state = frmc$resetState(world, pos, state);
        }

        BlockState aboveState = world.getBlockState(pos.above());
        if (!frmc$shouldDowngradeForAbove(aboveState)) {
            return;
        }

        int blockstate = frmc$getIntProperty(state, FRMC$BLOCKSTATE_PROPERTY);
        int downgradedBlockstate = frmc$getDowngradedStage(blockstate);
        if (downgradedBlockstate != blockstate) {
            frmc$setIntProperty(world, pos, state, FRMC$BLOCKSTATE_PROPERTY, downgradedBlockstate);
        }
    }

    @Unique
    private static boolean frmc$hasValidSupportBelow(BlockState belowState) {
        if (!belowState.is(ManorsBountyModBlocks.VINE_STAKE.get())) {
            return false;
        }

        return frmc$isSupportedStakeStage(frmc$getIntProperty(belowState, FRMC$BLOCKSTATE_PROPERTY));
    }

    @Unique
    private static boolean frmc$shouldDowngradeForAbove(BlockState aboveState) {
        if (aboveState.is(ManorsBountyModBlocks.VINE_ARBOR.get())) {
            return false;
        }

        if (!aboveState.is(ManorsBountyModBlocks.VINE_STAKE.get())) {
            return true;
        }

        return frmc$getIntProperty(aboveState, FRMC$BLOCKSTATE_PROPERTY) == 0;
    }

    @Unique
    private static boolean frmc$isSupportedStakeStage(int blockstate) {
        return blockstate == 2 || blockstate == 4 || blockstate == 6;
    }

    @Unique
    private static int frmc$getDowngradedStage(int blockstate) {
        return switch (blockstate) {
            case 2 -> 1;
            case 4 -> 3;
            case 6 -> 5;
            default -> blockstate;
        };
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
    private static BlockState frmc$resetState(LevelAccessor world, BlockPos pos, BlockState state) {
        BlockState updatedState = frmc$setStateValue(state, FRMC$BLOCKSTATE_PROPERTY, FRMC$RESET_VALUE);
        updatedState = frmc$setStateValue(updatedState, FRMC$AGE_PROPERTY, FRMC$RESET_VALUE);
        if (updatedState != state) {
            world.setBlock(pos, updatedState, 3);
        }
        return updatedState;
    }

    @Unique
    private static void frmc$setIntProperty(LevelAccessor world, BlockPos pos, BlockState state, String propertyName, int value) {
        BlockState updatedState = frmc$setStateValue(state, propertyName, value);
        if (updatedState != state) {
            world.setBlock(pos, updatedState, 3);
        }
    }

    @Unique
    private static BlockState frmc$setStateValue(BlockState state, String propertyName, int value) {
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

