package com.y271727uy.FRMC.mixin.alexscaves;

import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.integration.oculus.OculusIntegration;
import net.irisshaders.iris.api.v0.IrisApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.github.alexmodguy.alexscaves.client.render.entity.NucleeperRenderer$LayerGlow", remap = false)
public abstract class NucleeperGlowLayerMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", ordinal = 1, target =
            "Lcom/github/alexmodguy/alexscaves/client/render/ACRenderTypes;getEyesAlphaEnabled(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"),
            cancellable = true, require = 1)
    private void frmc$skipTransparentExplosionGlow(CallbackInfo ci, @Local(index = 12) float explodeProgress) {
        // The explosion glow is the final draw in this layer.
        if (explodeProgress == 0.0F && (!OculusIntegration.IS_IRIS_INSTALLED || !IrisApi.getInstance().isShaderPackInUse())) {
            ci.cancel();
        }
    }
}
