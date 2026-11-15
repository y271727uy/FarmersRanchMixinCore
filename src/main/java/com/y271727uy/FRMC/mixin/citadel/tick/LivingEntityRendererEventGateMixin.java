package com.y271727uy.FRMC.mixin.citadel.tick;

import com.bawnorton.mixinsquared.TargetHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.y271727uy.FRMC.integration.citadel.tick.CitadelRendererEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Citadel 的 LivingEntityRendererMixin 在每次渲染时构造并 post 4 个 EventLivingRenderer 事件，
 * 它们的处理方法只做"构造事件 → post → return"。这里用 MixinSquared 在 Citadel 的处理方法头部
 * 注入：事件总线上没有监听器时直接取消，省掉每实体每帧的对象分配和总线分发。
 * 监听器是实时探测的，别的模组之后才注册监听也会立刻生效。
 */
@Pseudo
@Mixin(value = LivingEntityRenderer.class, priority = 1500)
public abstract class LivingEntityRendererEventGateMixin {
    private static final String CITADEL_MIXIN = "com.github.alexthe666.citadel.mixin.client.LivingEntityRendererMixin";

    @Dynamic("Citadel LivingEntityRenderer setupRotations handler")
    @TargetHandler(mixin = CITADEL_MIXIN, name = "citadel_setupRotations")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void frmc$gateSetupRotations(
            LivingEntity entity, PoseStack poseStack, float bodyYRot, float yRot, float partialTick,
            CallbackInfo original, CallbackInfo citadel) {
        if (!CitadelRendererEvents.wantsSetupRotations()) {
            citadel.cancel();
        }
    }

    @Dynamic("Citadel LivingEntityRenderer pre setupAnim handler")
    @TargetHandler(mixin = CITADEL_MIXIN, name = "citadel_render_setupAnim_before")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void frmc$gatePreSetupAnimations(
            LivingEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int light, CallbackInfo original, CallbackInfo citadel) {
        if (!CitadelRendererEvents.wantsPreSetupAnimations()) {
            citadel.cancel();
        }
    }

    @Dynamic("Citadel LivingEntityRenderer post setupAnim handler")
    @TargetHandler(mixin = CITADEL_MIXIN, name = "citadel_render_setupAnim_after")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void frmc$gatePostSetupAnimations(
            LivingEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int light, CallbackInfo original, CallbackInfo citadel) {
        if (!CitadelRendererEvents.wantsPostSetupAnimations()) {
            citadel.cancel();
        }
    }

    @Dynamic("Citadel LivingEntityRenderer post renderToBuffer handler")
    @TargetHandler(mixin = CITADEL_MIXIN, name = "citadel_render_renderToBuffer")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void frmc$gatePostRenderModel(
            LivingEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int light, CallbackInfo original, CallbackInfo citadel) {
        if (!CitadelRendererEvents.wantsPostRenderModel()) {
            citadel.cancel();
        }
    }
}
