package com.y271727uy.FRMC.mixin.kaleidoscopecookery;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TeapotBlockEntity.class)
public abstract class TeapotBlockEntityMixin {
    @WrapOperation(method = "tick", remap = false, require = 2, at = {
            @At(value = "INVOKE", remap = false, ordinal = 0, target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/TeapotBlockEntity;refresh()V"),
            @At(value = "INVOKE", remap = false, ordinal = 3, target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/TeapotBlockEntity;refresh()V")})
    private void frmc$saveCountdownWithoutSendingNbt(TeapotBlockEntity teapot, Operation<Void> original,
                                                     @Local(argsOnly = true) Level level) {
        if (!level.isClientSide()) {
            level.blockEntityChanged(teapot.getBlockPos());
        }
    }

    @WrapOperation(method = "tick", remap = false, require = 1,
            at = @At(value = "INVOKE", remap = false, ordinal = 0, target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/TeapotBlockEntity;onProcessingEffects(Lnet/minecraft/world/level/Level;)V"))
    private void frmc$skipEffectsWithoutTeaLeaves(TeapotBlockEntity teapot, Level level, Operation<Void> original) {
        if (!teapot.getInput().isEmpty()) {
            original.call(teapot, level);
        }
    }
}
