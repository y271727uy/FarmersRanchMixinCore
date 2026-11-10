package com.y271727uy.FRMC.mixin.alexscaves;

import com.y271727uy.FRMC.integration.alexscaves.MagneticParticleBatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Keep the vanilla CUSTOM type so particle renderers that recognize it still use their usual path.
@Mixin(targets = "net.minecraft.client.particle.ParticleRenderType$5")
public abstract class CustomParticleRenderTypeMixin {
    @Inject(method = "begin", at = @At("TAIL"))
    private void frmc$beginMagneticParticleBatch(CallbackInfo ci) {
        MagneticParticleBatch.begin();
    }

    @Inject(method = "end", at = @At("HEAD"))
    private void frmc$submitMagneticParticleBatch(CallbackInfo ci) {
        MagneticParticleBatch.end();
    }
}
