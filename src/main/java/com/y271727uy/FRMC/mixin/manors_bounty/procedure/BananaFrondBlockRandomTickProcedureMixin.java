package com.y271727uy.FRMC.mixin.manors_bounty.procedure;

import net.mcreator.manors_bounty.init.ManorsBountyModBlocks;
import net.mcreator.manors_bounty.network.ManorsBountyModVariables.MapVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.mcreator.manors_bounty.procedures.BananaFrondBlockRandomTickProcedure")
public abstract class BananaFrondBlockRandomTickProcedureMixin {
    @Unique
    private static final String FRMC$BLOCKSTATE_PROPERTY = "blockstate";
    @Unique
    private static final String FRMC$AGE_PROPERTY = "age";
    @Unique
    private static final int FRMC$FROND_STAGE_IDLE = 0;
    @Unique
    private static final int FRMC$FROND_STAGE_FLOWERING = 1;
    @Unique
    private static final int FRMC$FROND_STAGE_FRUITING = 2;
    @Unique
    private static final int FRMC$MAX_BUNCH_STAGE = 2;
    @Unique
    private static final double FRMC$BLOSSOM_GROWTH_SCALE = 0.45D;
    @Unique
    private static final double FRMC$FRUIT_GROWTH_MULTIPLIER = 6.0D;
    @Unique
    private static final double FRMC$MAX_EFFECTIVE_CHANCE = 0.95D;

    /**
     * @author FRMC
     * @reason Replace the generated banana frond tick procedure with a compatibility-safe implementation that avoids repeated block/property lookups and slows down blossom/fruit growth.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockPos belowPos = pos.below();
        BlockPos twoBelowPos = pos.below(2);
        BlockPos threeBelowPos = pos.below(3);
        if (!frmc$isBananaStalk(world.getBlockState(belowPos))
            || !frmc$isBananaStalk(world.getBlockState(twoBelowPos))
            || !frmc$isBananaStalk(world.getBlockState(threeBelowPos))) {
            return;
        }

        int frondStage = frmc$getIntProperty(blockstate, FRMC$BLOCKSTATE_PROPERTY);
        if (frondStage < 0) {
            return;
        }

        BlockPos flowerPos = pos.offset(0, -2, 1);
        BlockPos bunchPos = pos.offset(0, -1, 1);
        BlockState flowerState = world.getBlockState(flowerPos);

        if (frondStage == FRMC$FROND_STAGE_IDLE) {
            if (!flowerState.canBeReplaced()) {
                return;
            }

            double blossomChance = frmc$clampChance(MapVariables.get(world).fruit_tree_blossom * FRMC$BLOSSOM_GROWTH_SCALE);
            if (Math.random() >= blossomChance) {
                return;
            }

            frmc$setIntProperty(world, pos, blockstate, FRMC$BLOCKSTATE_PROPERTY, FRMC$FROND_STAGE_FLOWERING);
            world.setBlock(flowerPos, ManorsBountyModBlocks.BANANA_TREE_FLOWER.get().defaultBlockState(), 3);
            return;
        }

        boolean hasFlower = flowerState.is(ManorsBountyModBlocks.BANANA_TREE_FLOWER.get());
        if (!hasFlower) {
            return;
        }

        BlockState bunchState = world.getBlockState(bunchPos);
        double fruitChance = frmc$clampChance(MapVariables.get(world).fruit_tree_fruit * FRMC$FRUIT_GROWTH_MULTIPLIER);

        if (frondStage == FRMC$FROND_STAGE_FLOWERING && bunchState.canBeReplaced()) {
            if (Math.random() >= fruitChance) {
                return;
            }

            world.setBlock(bunchPos, ManorsBountyModBlocks.BANANA_TREE_BUNCH.get().defaultBlockState(), 3);
            frmc$setIntProperty(world, pos, blockstate, FRMC$BLOCKSTATE_PROPERTY, FRMC$FROND_STAGE_FRUITING);
            return;
        }

        if (!bunchState.is(ManorsBountyModBlocks.BANANA_TREE_BUNCH.get()) || Math.random() >= fruitChance) {
            return;
        }

        int bunchStage = frmc$getIntProperty(bunchState, FRMC$BLOCKSTATE_PROPERTY);
        if (bunchStage < 0 || bunchStage >= FRMC$MAX_BUNCH_STAGE) {
            return;
        }

        frmc$setIntProperty(world, bunchPos, bunchState, FRMC$BLOCKSTATE_PROPERTY, bunchStage + 1);
        frmc$incrementIntProperty(world, flowerPos, flowerState, FRMC$AGE_PROPERTY);
    }

    @Unique
    private static boolean frmc$isBananaStalk(BlockState state) {
        return state.is(ManorsBountyModBlocks.BANANA_TREE_STALK.get());
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
    private static void frmc$incrementIntProperty(LevelAccessor world, BlockPos pos, BlockState state, String propertyName) {
        int currentValue = frmc$getIntProperty(state, propertyName);
        if (currentValue < 0) {
            return;
        }
        frmc$setIntProperty(world, pos, state, propertyName, currentValue + 1);
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

    @Unique
    private static double frmc$clampChance(double chance) {
        if (chance <= 0.0D) {
            return 0.0D;
        }
        return Math.min(chance, FRMC$MAX_EFFECTIVE_CHANCE);
    }
}
