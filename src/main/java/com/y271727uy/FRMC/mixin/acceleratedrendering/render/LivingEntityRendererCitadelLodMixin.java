package com.y271727uy.FRMC.mixin.acceleratedrendering.render;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.config.Config;
import com.y271727uy.FRMC.integration.citadel.render.ar.CitadelModelLod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererCitadelLodMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            require = 0)
    private void frmc$scopeCitadelBaseModel(EntityModel<?> model, PoseStack poses, VertexConsumer consumer,
                                            int light, int overlay, float red, float green, float blue, float alpha,
                                            Operation<Void> original) {
        EntityModel<?> previous = CitadelModelLod.setBaseModel(Config.citadelGeneralLodEnabled
                && model instanceof AdvancedEntityModel<?> && alpha == 1.0F ? model : null);
        try {
            original.call(model, poses, consumer, light, overlay, red, green, blue, alpha);
        } finally {
            CitadelModelLod.setBaseModel(previous);
        }
    }
}
