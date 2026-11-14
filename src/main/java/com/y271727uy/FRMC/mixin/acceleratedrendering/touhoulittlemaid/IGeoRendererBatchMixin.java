package com.y271727uy.FRMC.mixin.acceleratedrendering.touhoulittlemaid;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoReplacedEntityRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoModel;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.acceleratedrendering.MaidModelBatch;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GeoReplacedEntityRenderer.class, remap = false)
public abstract class IGeoRendererBatchMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target =
            "Lcom/github/tartaricacid/touhoulittlemaid/geckolib3/geo/GeoReplacedEntityRenderer;render("
            + "Lcom/github/tartaricacid/touhoulittlemaid/geckolib3/geo/animated/AnimatedGeoModel;"
            + "Ljava/lang/Object;FLnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            require = 1)
    private void frmc$batchMaidModel(GeoReplacedEntityRenderer<?, ?> renderer, AnimatedGeoModel model,
                                     Object entity, float partialTick, RenderType renderType, PoseStack pose,
                                     MultiBufferSource buffers, VertexConsumer buffer, int light, int overlay,
                                     float red, float green, float blue, float alpha, Operation<Void> original) {
        MaidModelBatch batch = MaidModelBatch.begin(renderer, model, pose, light);
        try {
            original.call(renderer, model, entity, partialTick, renderType, pose, buffers, buffer,
                    light, overlay, red, green, blue, alpha);
            if (batch != null) {
                batch.submit();
            }
        } finally {
            if (batch != null) {
                batch.clear();
            }
        }
    }
}
