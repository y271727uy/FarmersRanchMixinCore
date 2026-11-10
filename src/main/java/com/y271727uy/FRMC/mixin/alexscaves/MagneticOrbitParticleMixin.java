package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.client.particle.AbstractTrailParticle;
import com.github.alexmodguy.alexscaves.client.particle.MagneticOrbitParticle;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.alexscaves.MagneticParticleBatch;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractTrailParticle.class)
public abstract class MagneticOrbitParticleMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"))
    private MultiBufferSource.BufferSource frmc$useOrbitBatch(RenderBuffers renderBuffers,
            Operation<MultiBufferSource.BufferSource> original) {
        if ((Object) this instanceof MagneticOrbitParticle) {
            MultiBufferSource.BufferSource batch = MagneticParticleBatch.getOrbitBuffers();
            if (batch != null) {
                return batch;
            }
        }
        return original.call(renderBuffers);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V"))
    private void frmc$deferOrbitSubmission(MultiBufferSource.BufferSource buffers, Operation<Void> original) {
        if (!MagneticParticleBatch.owns(buffers)) {
            original.call(buffers);
        }
    }
}
