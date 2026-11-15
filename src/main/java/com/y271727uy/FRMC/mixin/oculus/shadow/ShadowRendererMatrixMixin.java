package com.y271727uy.FRMC.mixin.oculus.shadow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.y271727uy.FRMC.integration.oculus.ShadowUpdateInterval;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.uniforms.CameraUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * createShadowModelView 既用于渲染阴影，也是着色器 shadowModelView uniform 的来源。
 * 正常帧记录它的结果；跳过的帧（此时只有 uniform 会调用它）换成带相机位移补偿的旧矩阵。
 */
@Mixin(value = ShadowRenderer.class, remap = false)
public abstract class ShadowRendererMatrixMixin {
    @Inject(method = "createShadowModelView", at = @At("RETURN"), cancellable = true)
    private static void frmc$reuseShadowModelView(float sunPathRotation, float intervalSize, CallbackInfoReturnable<PoseStack> cir) {
        if (ShadowUpdateInterval.skipping()) {
            cir.setReturnValue(ShadowUpdateInterval.compensated(CameraUniforms.getUnshiftedCameraPosition()));
        } else {
            ShadowUpdateInterval.capture(cir.getReturnValue().last(), CameraUniforms.getUnshiftedCameraPosition());
        }
    }
}
