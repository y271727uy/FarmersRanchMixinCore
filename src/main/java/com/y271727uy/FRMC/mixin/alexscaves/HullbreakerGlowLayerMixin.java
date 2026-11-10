package com.y271727uy.FRMC.mixin.alexscaves;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.alexscaves.HullbreakerRenderMaterials;
import com.y271727uy.FRMC.integration.oculus.OculusIntegration;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.github.alexmodguy.alexscaves.client.render.entity.HullbreakerRenderer$LayerGlow", remap = false)
public abstract class HullbreakerGlowLayerMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target =
            "Lcom/github/alexmodguy/alexscaves/client/render/ACRenderTypes;getEyesAlphaEnabled(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"),
            require = 1)
    private RenderType frmc$reuseGlowType(ResourceLocation texture, Operation<RenderType> original) {
        if (OculusIntegration.IS_IRIS_INSTALLED && IrisApi.getInstance().isShaderPackInUse()) {
            return original.call(texture);
        }
        return HullbreakerRenderMaterials.glow(() -> original.call(texture));
    }
}
