package com.y271727uy.FRMC.mixin.manors_bounty.client;

import com.y271727uy.FRMC.integration.manors_bounty.HoundstoothCarpetModels;
import net.mcreator.manors_bounty.block.HoundstoothInfiniteCarpetBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(HoundstoothInfiniteCarpetBlock.class)
public abstract class HoundstoothCarpetBlockMixin {
    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void frmc$renderStaticCarpetInChunk(BlockState state, CallbackInfoReturnable<RenderShape> cir) {
        if (HoundstoothCarpetModels.hasBakedModels() && state.getValue(HoundstoothInfiniteCarpetBlock.ANIMATION) == 0) {
            cir.setReturnValue(RenderShape.MODEL);
        }
    }
}
