package com.y271727uy.FRMC.mixin.manors_bounty;

import net.mcreator.manors_bounty.init.ManorsBountyModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

@Mixin(targets = "net.mcreator.manors_bounty.procedures.YoungCoconutsRandomTickProcedure")
public abstract class YoungCoconutsRandomTickProcedureMixin {
    @Unique
    private static final int FRMC$MAX_STAGE = 3;
    @Unique
    private static final double FRMC$GROWTH_CHANCE = 0.075D;
    @Unique
    private static final ResourceLocation FRMC$WOOD_BREAK_SOUND_ID = Objects.requireNonNull(ResourceLocation.tryParse("minecraft:block.wood.break"));


    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
        BlockPos pos = BlockPos.containing(x, y, z);

        if (!frmc$hasLeavesAbove(world, x, y, z)) {
            Block.dropResources(world.getBlockState(pos), world, BlockPos.containing(x + 0.5D, y, z + 0.5D), null);
            world.destroyBlock(pos, false);
            return;
        }

        int stage = frmc$getStage(blockstate);
        if (stage < 0) {
            return;
        }

        if (stage < FRMC$MAX_STAGE) {
            if (Math.random() < FRMC$GROWTH_CHANCE) {
                frmc$setStage(world, pos, stage + 1);
            }
            return;
        }

        BlockPos belowPos = pos.below();
        BlockPos twoBelowPos = pos.below(2);
        boolean canDropBelow = world.getBlockState(belowPos).canBeReplaced();
        boolean canDropTwoBelow = world.getBlockState(twoBelowPos).canBeReplaced();
        if (!canDropBelow && !canDropTwoBelow) {
            return;
        }

        frmc$setStage(world, pos, 0);

        if (world instanceof ServerLevel serverLevel) {
            if (canDropBelow) {
                FallingBlockEntity.fall(serverLevel, belowPos, ManorsBountyModBlocks.YOUNG_COCONUT.get().defaultBlockState());
            }
            if (canDropTwoBelow) {
                FallingBlockEntity.fall(serverLevel, twoBelowPos, ManorsBountyModBlocks.YOUNG_COCONUT.get().defaultBlockState());
            }
        }

        frmc$playBreakSound(world, x, y, z, pos);
    }

    @Unique
    private static boolean frmc$hasLeavesAbove(LevelAccessor world, double x, double y, double z) {
        return world.getBlockState(BlockPos.containing(x, y + 1.0D, z)).getBlock() == ManorsBountyModBlocks.COCONUT_TREE_LEAVES.get();
    }

    @Unique
    private static int frmc$getStage(BlockState state) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty("blockstate");
        if (!(property instanceof IntegerProperty integerProperty)) {
            return -1;
        }
        return state.getValue(integerProperty);
    }

    @Unique
    private static void frmc$setStage(LevelAccessor world, BlockPos pos, int value) {
        BlockState state = world.getBlockState(pos);
        Property<?> property = state.getBlock().getStateDefinition().getProperty("blockstate");
        if (!(property instanceof IntegerProperty integerProperty)) {
            return;
        }
        if (!integerProperty.getPossibleValues().contains(value)) {
            return;
        }
        world.setBlock(pos, state.setValue(integerProperty, value), 3);
    }

    @Unique
    private static void frmc$playBreakSound(LevelAccessor world, double x, double y, double z, BlockPos pos) {
        if (!(world instanceof Level level)) {
            return;
        }

        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(FRMC$WOOD_BREAK_SOUND_ID);
        if (sound == null) {
            return;
        }

        if (level.isClientSide()) {
            level.playLocalSound(x, y, z, sound, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        } else {
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}


