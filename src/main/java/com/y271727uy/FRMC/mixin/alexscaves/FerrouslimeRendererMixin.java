package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.client.render.entity.FerrouslimeRenderer;
import com.github.alexmodguy.alexscaves.server.entity.living.FerrouslimeEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(FerrouslimeRenderer.class)
public abstract class FerrouslimeRendererMixin {
    @WrapOperation(method = "render", remap = false, at = @At(value = "INVOKE", remap = false, target =
            "Lcom/github/alexmodguy/alexscaves/client/render/ACRenderTypes;getGelTriangles(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"),
            require = 1)
    private RenderType frmc$skipEmptySpikeType(ResourceLocation texture, Operation<RenderType> original,
            @Local(argsOnly = true) FerrouslimeEntity entity,
            @Local(argsOnly = true, ordinal = 1) float partialTick,
            @Local(index = 10) float gelSize) {
        float spikeLength = (gelSize + 1.0F) * entity.getAttackProgress(partialTick);
        return spikeLength > 0.0F ? original.call(texture) : null;
    }

    @WrapOperation(method = "render", slice = @Slice(from = @At(value = "INVOKE", remap = false, target =
            "Lcom/github/alexmodguy/alexscaves/client/render/ACRenderTypes;getGelTriangles(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;")),
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
            require = 1)
    private VertexConsumer frmc$skipEmptySpikeBuffer(MultiBufferSource buffers, RenderType type,
            Operation<VertexConsumer> original) {
        return type == null ? null : original.call(buffers, type);
    }

    @WrapOperation(method = "render", remap = false, at = @At(value = "INVOKE", remap = false, target =
            "Lcom/github/alexmodguy/alexscaves/client/render/entity/FerrouslimeRenderer;renderGelSpikes(Lcom/github/alexmodguy/alexscaves/server/entity/living/FerrouslimeEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;FI)V"),
            require = 1)
    private void frmc$skipEmptySpikeDraw(FerrouslimeRenderer renderer, FerrouslimeEntity entity, float partialTick,
            PoseStack poses, VertexConsumer consumer, float gelSize, int light, Operation<Void> original) {
        if (consumer != null) {
            original.call(renderer, entity, partialTick, poses, consumer, gelSize, light);
        }
    }
}
