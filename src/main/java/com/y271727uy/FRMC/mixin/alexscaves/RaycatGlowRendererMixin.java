package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.client.render.entity.RaycatRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.oculus.OculusIntegration;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RaycatRenderer.class, remap = false)
public abstract class RaycatGlowRendererMixin {
    @Shadow(remap = false) @Final private static ResourceLocation TEXTURE_BODY;
    @Unique private RenderType frmc$bodyGlow;

    @WrapOperation(method = "getRenderType", at = @At(value = "INVOKE", target =
            "Lcom/github/alexmodguy/alexscaves/client/render/ACRenderTypes;getRadiationGlow(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"),
            require = 1)
    private RenderType frmc$reuseBodyGlow(ResourceLocation texture, Operation<RenderType> original) {
        if (!TEXTURE_BODY.equals(texture) || (OculusIntegration.IS_IRIS_INSTALLED && IrisApi.getInstance().isShaderPackInUse())) {
            return original.call(texture);
        }
        if (frmc$bodyGlow == null) {
            frmc$bodyGlow = original.call(texture);
        }
        return frmc$bodyGlow;
    }
}
