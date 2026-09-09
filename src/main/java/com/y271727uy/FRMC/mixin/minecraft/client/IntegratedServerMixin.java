package com.y271727uy.FRMC.mixin.minecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class IntegratedServerMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void frmc$setIntegratedServerPriority(GameConfig gameConfig, CallbackInfo ci) {
        Thread.currentThread().setPriority(readInt("integratedServerPriority", 5, 1, 10));
    }

    private static int readInt(String key, int fallback, int min, int max) {
        try {
            String value = System.getProperty("frmc.smoothboot." + key);
            return value == null ? fallback : Math.max(min, Math.min(max, Integer.parseInt(value)));
        } catch (RuntimeException ignored) { return fallback; }
    }
}
