package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.client.particle.MagnetLightningParticle;
import com.github.alexmodguy.alexscaves.client.particle.TeslaBulbLightningParticle;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.alexscaves.MagneticParticleBatch;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({MagnetLightningParticle.class, TeslaBulbLightningParticle.class})
public abstract class LightningParticleMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"))
    private MultiBufferSource.BufferSource frmc$useLightningBatch(RenderBuffers renderBuffers,
            Operation<MultiBufferSource.BufferSource> original) {
        MultiBufferSource.BufferSource batch = MagneticParticleBatch.getLightningBuffers();
        return batch != null ? batch : original.call(renderBuffers);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V"))
    private void frmc$deferLightningSubmission(MultiBufferSource.BufferSource buffers, Operation<Void> original) {
        if (!MagneticParticleBatch.owns(buffers)) {
            original.call(buffers);
        }
    }
}
