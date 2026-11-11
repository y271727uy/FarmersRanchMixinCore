package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.client.particle.RainbowParticle;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.alexscaves.RainbowParticleBatch;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RainbowParticle.class)
public abstract class RainbowParticleMixin {
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V"), require = 1)
    private boolean frmc$skipRainbowSegmentPush(PoseStack pose) {
        // The quad loop uses the same transform for every segment.
        return false;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"), require = 1)
    private boolean frmc$skipRainbowSegmentPop(PoseStack pose) {
        return false;
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
            require = 1)
    private VertexConsumer frmc$prepareRainbowBuffer(MultiBufferSource.BufferSource buffers,
            RenderType type, Operation<VertexConsumer> original) {
        RainbowParticleBatch.beforeBuffer(this, buffers);
        return original.call(buffers, type);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V"), require = 1)
    private void frmc$deferRainbowSubmission(MultiBufferSource.BufferSource buffers, Operation<Void> original) {
        if (!RainbowParticleBatch.defer(this, buffers)) {
            original.call(buffers);
        }
    }
}
