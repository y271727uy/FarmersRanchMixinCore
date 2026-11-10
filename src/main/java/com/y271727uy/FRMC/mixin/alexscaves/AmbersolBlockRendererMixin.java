package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.client.render.blockentity.AmbersolBlockRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.oculus.OculusIntegration;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.irisshaders.iris.api.v0.IrisApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = AmbersolBlockRenderer.class, remap = false)
public abstract class AmbersolBlockRendererMixin {
    @Unique private static MultiBufferSource.BufferSource frmc$shineBuffers;
    @Unique private static RenderType frmc$shineType;

    @WrapOperation(method = "renderEntireBatch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;",
            remap = true))
    private static MultiBufferSource.BufferSource frmc$useShineBuffers(RenderBuffers buffers,
                                                                       Operation<MultiBufferSource.BufferSource> original) {
        if (OculusIntegration.IS_IRIS_INSTALLED && IrisApi.getInstance().isShaderPackInUse()) {
            return original.call(buffers);
        }
        if (frmc$shineBuffers == null) {
            frmc$shineBuffers = MultiBufferSource.immediate(new BufferBuilder(256));
        }
        return frmc$shineBuffers;
    }

    @WrapOperation(method = "renderAt", at = @At(value = "INVOKE",
            target = "Lcom/github/alexmodguy/alexscaves/client/render/ACRenderTypes;getAmbersolShine()Lnet/minecraft/client/renderer/RenderType;"))
    private static RenderType frmc$reuseShineType(Operation<RenderType> original,
                                                 @Local(argsOnly = true) MultiBufferSource buffers) {
        if (buffers != frmc$shineBuffers) {
            return original.call();
        }
        if (frmc$shineType == null) {
            frmc$shineType = original.call();
        }
        return frmc$shineType;
    }

    @WrapOperation(method = "renderAt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            ordinal = 1, remap = true))
    private static VertexConsumer frmc$skipEmptyLineBuffer(MultiBufferSource buffers, RenderType lines,
                                                           Operation<VertexConsumer> original) {
        if (buffers != frmc$shineBuffers) {
            return original.call(buffers, lines);
        }
        // Alex's Caves discards this consumer. The type switch only flushed the preceding light.
        return null;
    }

    @Inject(method = "renderEntireBatch", at = @At("RETURN"))
    private static void frmc$submitShines(CallbackInfo ci) {
        if (frmc$shineBuffers != null && frmc$shineType != null) {
            frmc$shineBuffers.endBatch(frmc$shineType);
        }
    }
}
