package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig;
import com.y271727uy.FRMC.integration.alexscaves.CaveBiomeHookGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BiomeGenerationConfig.class, remap = false)
public abstract class BiomeGenerationConfigReloadMixin {
    @Inject(method = "reloadConfig", at = @At("TAIL"))
    private static void frmc$invalidateCaveBiomeGate(CallbackInfo ci) {
        CaveBiomeHookGate.invalidate();
    }
}
