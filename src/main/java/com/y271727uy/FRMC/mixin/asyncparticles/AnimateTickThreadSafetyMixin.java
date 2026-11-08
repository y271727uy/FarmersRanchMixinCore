package com.y271727uy.FRMC.mixin.asyncparticles;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps world-backed ClientLevel animateTick work on the render thread. */
@Pseudo
@Mixin(targets = "forge.fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper", remap = false)
public abstract class AnimateTickThreadSafetyMixin {
    @Inject(method = "asyncBlockEntityAnimate()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private static void frmc$disableAsyncWorldAnimation(CallbackInfoReturnable<Boolean> callbackInfo) {
        callbackInfo.setReturnValue(false);
    }
}
