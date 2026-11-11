package com.y271727uy.FRMC.mixin.alexscaves;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.integration.alexscaves.RainbowParticleBatch;
import java.util.Iterator;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ParticleEngine.class)
public abstract class RainbowParticleEngineMixin {
    // Forge adds this overload. The second iterator walks particles within the current render type.
    @WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
            at = @At(value = "INVOKE", ordinal = 1, target = "Ljava/util/Iterator;next()Ljava/lang/Object;"),
            remap = false, require = 1)
    private Object frmc$flushBeforeOtherParticles(Iterator<?> particles, Operation<Object> original,
            @Local ParticleRenderType type) {
        Object particle = original.call(particles);
        if (type == ParticleRenderType.CUSTOM) {
            RainbowParticleBatch.beforeParticle(particle);
        }
        return particle;
    }
}
