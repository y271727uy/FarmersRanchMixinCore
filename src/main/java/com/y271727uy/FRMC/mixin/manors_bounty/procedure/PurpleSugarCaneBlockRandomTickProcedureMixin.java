package com.y271727uy.FRMC.mixin.manors_bounty.procedure;

import net.mcreator.manors_bounty.init.ManorsBountyModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.mcreator.manors_bounty.procedures.PurpleSugarCaneBlockRandomTickProcedure")
public abstract class PurpleSugarCaneBlockRandomTickProcedureMixin {
    @Unique
    private static final String FRMC$AGE_PROPERTY = "age";
    @Unique
    private static final String FRMC$BLOCKSTATE_PROPERTY = "blockstate";
    @Unique
    private static final int FRMC$MAX_AGE = 7;
    @Unique
    private static final int FRMC$GROWN_BLOCKSTATE = 1;
    @Unique
    private static final int FRMC$FAST_GROWTH_THRESHOLD = 5;
    @Unique
    private static final double FRMC$GROWTH_CHANCE = 0.25D;
    @Unique
    private static final double FRMC$FAST_GROWTH_CHANCE = 0.15D;

    /**
     * @author FRMC
     * @reason Replace the generated sugar cane tick procedure with a cleaner and lower-overhead implementation.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
        BlockPos basePos = BlockPos.containing(x, y, z);
        BlockPos abovePos = basePos.above();
        BlockState aboveState = world.getBlockState(abovePos);
        if (!aboveState.canBeReplaced()) {
            return;
        }

        int currentAge = frmc$getAge(blockstate);
        if (currentAge < 0 || currentAge >= FRMC$MAX_AGE || Math.random() >= FRMC$GROWTH_CHANCE) {
            return;
        }

        frmc$setGrownBlockstate(world, basePos);

        BlockState newAboveState = ManorsBountyModBlocks.PURPLE_SUGAR_CANE_BLOCK.get().defaultBlockState();
        world.setBlock(abovePos, newAboveState, 3);

        int nextAge = currentAge + 1;
        BlockState placedAboveState = world.getBlockState(abovePos);
        frmc$setAge(world, abovePos, placedAboveState, nextAge);

        if (nextAge >= FRMC$FAST_GROWTH_THRESHOLD && Math.random() < FRMC$FAST_GROWTH_CHANCE) {
            frmc$setAge(world, abovePos, world.getBlockState(abovePos), FRMC$MAX_AGE);
        }
    }

    @Unique
    private static int frmc$getAge(BlockState state) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(FRMC$AGE_PROPERTY);
        if (!(property instanceof IntegerProperty integerProperty)) {
            return -1;
        }
        return state.getValue(integerProperty);
    }

    @Unique
    private static void frmc$setGrownBlockstate(LevelAccessor world, BlockPos pos) {
        frmc$setProperty(world, pos, world.getBlockState(pos), FRMC$BLOCKSTATE_PROPERTY, FRMC$GROWN_BLOCKSTATE);
    }

    @Unique
    private static void frmc$setAge(LevelAccessor world, BlockPos pos, BlockState state, int value) {
        frmc$setProperty(world, pos, state, FRMC$AGE_PROPERTY, value);
    }

    @Unique
    private static void frmc$setProperty(LevelAccessor world, BlockPos pos, BlockState state, String propertyName, int value) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(propertyName);
        if (!(property instanceof IntegerProperty integerProperty)) {
            return;
        }
        if (!integerProperty.getPossibleValues().contains(value)) {
            return;
        }
        world.setBlock(pos, state.setValue(integerProperty, value), 3);
    }
}




