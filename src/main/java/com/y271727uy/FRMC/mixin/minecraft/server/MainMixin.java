package com.y271727uy.FRMC.mixin.minecraft.server;

import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public abstract class MainMixin {
    @Inject(method = "main", at = @At("HEAD"))
    private static void frmc$setGamePriority(CallbackInfo ci) {
        Thread.currentThread().setPriority(readInt("gamePriority", 5, 1, 10));
    }

    private static int readInt(String key, int fallback, int min, int max) {
        try {
            String value = System.getProperty("frmc.smoothboot." + key);
            return value == null ? fallback : Math.max(min, Math.min(max, Integer.parseInt(value)));
        } catch (RuntimeException ignored) { return fallback; }
    }
}
