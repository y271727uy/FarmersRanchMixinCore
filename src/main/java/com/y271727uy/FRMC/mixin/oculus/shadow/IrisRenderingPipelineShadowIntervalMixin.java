package com.y271727uy.FRMC.mixin.oculus.shadow;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.oculus.ShadowUpdateInterval;
import net.irisshaders.iris.gl.image.GlImage;
import net.irisshaders.iris.gl.program.ComputeProgram;
import net.irisshaders.iris.helpers.OptionalBoolean;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.PackShadowDirectives;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.uniforms.CameraUniforms;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 阴影更新间隔。跳过的帧要同时跳过两件事：帧开头清空阴影贴图，以及之后的阴影渲染。
 * 三处注入必须同时成立（只跳过清空或只跳过渲染都会出错），所以不用 require = 0，
 * 改由 FRMCMixinLoadConfig 限定只在核对过的 Oculus 1.8.0 上加载。
 */
@Mixin(value = IrisRenderingPipeline.class, remap = false)
public abstract class IrisRenderingPipelineShadowIntervalMixin {
    @Shadow
    private ShadowRenderTargets shadowRenderTargets;

    @Shadow
    @Final
    private GlImage[] clearImages;

    @Shadow
    @Final
    private ComputeProgram[] shadowComputes;

    @Inject(method = "beginLevelRendering", at = @At("HEAD"))
    private void frmc$beginShadowIntervalFrame(CallbackInfo ci) {
        ShadowUpdateInterval.beginFrame();
    }

    /**
     * beginLevelRendering 里“光影包禁用了阴影”的分支只在需要完整清空时才动阴影贴图，
     * 跳过的帧借用这个分支，就不会执行清深度、阴影计算着色器和清空通道。
     */
    @WrapOperation(
        method = "beginLevelRendering",
        at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/shaderpack/properties/PackShadowDirectives;isShadowEnabled()Lnet/irisshaders/iris/helpers/OptionalBoolean;"
        )
    )
    private OptionalBoolean frmc$skipShadowClear(PackShadowDirectives directives, Operation<OptionalBoolean> original) {
        OptionalBoolean enabled = original.call(directives);
        if (enabled == OptionalBoolean.FALSE) {
            return enabled;
        }

        boolean skip = ShadowUpdateInterval.decide(this, this.frmc$mustRenderShadows(), CameraUniforms.getUnshiftedCameraPosition());
        return skip ? OptionalBoolean.FALSE : enabled;
    }

    @WrapOperation(
        method = "renderShadows",
        at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/shadows/ShadowRenderer;renderShadows(Lnet/irisshaders/iris/mixin/LevelRendererAccessor;Lnet/minecraft/client/Camera;)V"
        )
    )
    private void frmc$skipShadowRender(ShadowRenderer renderer, LevelRendererAccessor levelRenderer, Camera camera, Operation<Void> original) {
        if (!ShadowUpdateInterval.skipping()) {
            original.call(renderer, levelRenderer, camera);
        }
    }

    /**
     * 保守处理：光影包如果有每帧清空的自定义图像，或者有阴影计算着色器，
     * 它们很可能依赖阴影遍每帧写入（比如体素化的彩色光照），这类光影包不跳帧。
     */
    @Unique
    private boolean frmc$mustRenderShadows() {
        if (this.shadowRenderTargets.isFullClearRequired() || this.clearImages.length > 0) {
            return true;
        }
        for (ComputeProgram program : this.shadowComputes) {
            if (program != null) {
                return true;
            }
        }
        return false;
    }
}
