package com.y271727uy.FRMC.mixin.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.capability.particle.cutting.ParticleCullingPolicy;
import com.y271727uy.FRMC.mixin.minecraft.client.renderer.LevelRendererAccessorMixin;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Applies the particle visibility policy at the final vanilla draw call. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineCullingMixin {
    private static final double MAX_PARTICLE_DISTANCE_SQR = 256.0D * 256.0D;

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"
            )
    )
    private void frmc$renderVisibleParticle(Particle particle, VertexConsumer vertexConsumer,
                                             Camera camera, float partialTick) {
        Frustum frustum = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.levelRenderer != null) {
            frustum = ((LevelRendererAccessorMixin) minecraft.levelRenderer).frmc$getCullingFrustum();
        }
        if (!ParticleCullingPolicy.shouldCull(
                particle.getBoundingBox(), camera.getPosition(), frustum,
                MAX_PARTICLE_DISTANCE_SQR, false)) {
            particle.render(vertexConsumer, camera, partialTick);
        }
    }
}
