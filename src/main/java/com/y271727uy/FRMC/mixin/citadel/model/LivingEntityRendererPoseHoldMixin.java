package com.y271727uy.FRMC.mixin.citadel.model;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.citadel.anim.CitadelPoseHold;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 屏幕上很小的 Citadel 实体每个 tick 只执行一次 {@code setupAnim}，其余帧写回它自己的姿态。
 * Citadel 在这次调用前后发出的 Pre/PostSetupAnimations 事件不受影响，仍然每帧触发。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererPoseHoldMixin {
    @WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"),
            require = 0)
    private void frmc$holdDistantCitadelPose(EntityModel<?> model, Entity entity, float limbSwing, float limbSwingAmount,
                                             float ageInTicks, float netHeadYaw, float headPitch,
                                             Operation<Void> original) {
        CitadelPoseHold.Ticket ticket = entity instanceof LivingEntity living
                ? CitadelPoseHold.begin(model, living) : null;
        if (ticket == null) {
            original.call(model, entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            return;
        }
        if (ticket.held()) {
            return;
        }
        original.call(model, entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        ticket.capture();
    }
}
