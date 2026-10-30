package com.y271727uy.FRMC.mixin.ruok.particlerain;

import com.leclowndu93150.particlerain.particle.WeatherParticle;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.teampotato.ruok.util.particle.ParticleRender;

/**
 * RuOK's add-time camera cull cancels {@code ParticleEngine.add} without
 * {@code Particle.remove()}. Particle Rain reserves its spawn budget in the
 * {@link WeatherParticle} constructor and only releases it from {@code remove()},
 * so rejected rain, snow and dust particles stay allocated until
 * {@code maxParticleAmount} is full and live precipitation fades out.
 */
@Mixin(value = ParticleRender.class, remap = false)
public abstract class ParticleRenderMixin {
    @Inject(
            method = "onAddParticleRaw(Lnet/minecraft/client/particle/Particle;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void frmc$skipWeatherParticleCull(Particle particle, CallbackInfo addCallback, CallbackInfo ci) {
        if (particle instanceof WeatherParticle) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onAddParticleRaw(Lnet/minecraft/client/particle/Particle;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",
            at = @At("RETURN"),
            remap = false
    )
    private static void frmc$releaseRejectedWeatherParticle(Particle particle, CallbackInfo addCallback, CallbackInfo ci) {
        if (addCallback.isCancelled() && particle instanceof WeatherParticle) {
            particle.remove();
        }
    }
}
