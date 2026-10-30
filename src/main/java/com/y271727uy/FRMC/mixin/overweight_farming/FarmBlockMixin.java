package com.y271727uy.FRMC.mixin.overweight_farming;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FarmBlock.class)
public abstract class FarmBlockMixin {
    @Unique
    private static final TagKey<Block> FRMC$OVERWEIGHT_CROPS = BlockTags.create(new ResourceLocation("frmc", "overweight_crops"));

    @Inject(method = "canSurvive", at = @At("RETURN"), cancellable = true)
    private void frmc$allowOverweightCropsSurvive(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            BlockState above = level.getBlockState(pos.above());
            if (frmc$isNonDestructiveOverweightCrop(above)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "shouldMaintainFarmland", at = @At("RETURN"), cancellable = true)
    private static void frmc$maintainFarmlandForOverweightCrops(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            BlockState above = level.getBlockState(pos.above());
            if (frmc$isNonDestructiveOverweightCrop(above)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Unique
    private static boolean frmc$isNonDestructiveOverweightCrop(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        if (state.is(FRMC$OVERWEIGHT_CROPS)) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id != null && "overweight_farming".equals(id.getNamespace())) {
            String path = id.getPath();
            // 排除生长在树木、灵魂沙上的非农田作物以及堆肥桶、花盆等
            if (path.contains("apple") || path.contains("cocoa") || path.contains("nether_wart")
                    || path.contains("potted") || path.contains("compost")) {
                return false;
            }
            return path.contains("overweight") || path.contains("peeled_melon") || path.contains("allium_bush");
        }
        return false;
    }
}
