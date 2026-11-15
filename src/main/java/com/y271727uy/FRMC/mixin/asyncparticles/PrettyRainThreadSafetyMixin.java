package com.y271727uy.FRMC.mixin.asyncparticles;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AsyncParticles 20.1.4.6 gates ParticleSpawner.tick and vanilla rain updates with
 * this helper. Keep those world-backed callbacks on their original client-thread
 * path without changing isAsyncParticleTick or deferring work across tick phases.
 */
@Pseudo
@Mixin(targets = "forge.fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper", remap = false)
public abstract class PrettyRainThreadSafetyMixin {
    @Inject(method = "isTickWeatherAsync()Z", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void frmc$disableAsyncWeatherTick(CallbackInfoReturnable<Boolean> callbackInfo) {
        callbackInfo.setReturnValue(false);
    }
}
