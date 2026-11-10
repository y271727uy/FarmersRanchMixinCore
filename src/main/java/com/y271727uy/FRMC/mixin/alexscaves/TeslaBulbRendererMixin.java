package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.client.render.blockentity.TelsaBulbBlockRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.oculus.OculusIntegration;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = TelsaBulbBlockRenderer.class, remap = false)
public abstract class TeslaBulbRendererMixin {
    @Unique private static RenderType frmc$bulbType;

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target =
            "Lcom/github/alexmodguy/alexscaves/client/render/ACRenderTypes;getTeslaBulb(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"),
            require = 1)
    private RenderType frmc$reuseBulbType(ResourceLocation texture, Operation<RenderType> original) {
        if (OculusIntegration.IS_IRIS_INSTALLED && IrisApi.getInstance().isShaderPackInUse()) {
            return original.call(texture);
        }
        if (frmc$bulbType == null) {
            frmc$bulbType = original.call(texture);
        }
        return frmc$bulbType;
    }
}
