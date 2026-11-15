package com.y271727uy.FRMC.mixin.xaero.worldmap;

import com.y271727uy.FRMC.integration.xaero.XaeroPerformanceConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapLimiter;

@Mixin(value = MapLimiter.class, remap = false)
public abstract class MapLimiterPollingMixin {
    @Unique private long frmc$lastVramPoll;
    @Unique private boolean frmc$hasPolled;

    @Inject(method = "updateAvailableVRAM", at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$limitDriverQueries(CallbackInfo ci) {
        int interval = XaeroPerformanceConfig.vramPollIntervalMillis();
        if (interval <= 0) {
            frmc$hasPolled = false;
            return;
        }
        long now = System.nanoTime();
        if (frmc$hasPolled && now - frmc$lastVramPoll < interval * 1_000_000L) {
            ci.cancel();
            return;
        }
        frmc$lastVramPoll = now;
        frmc$hasPolled = true;
    }
}
