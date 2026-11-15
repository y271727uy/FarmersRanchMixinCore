package com.y271727uy.FRMC.mixin.xaero.worldmap;

import com.y271727uy.FRMC.integration.xaero.XaeroPerformanceConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.graphics.TextureUploadBenchmark;

@Mixin(value = TextureUploadBenchmark.class, remap = false)
public abstract class TextureUploadBenchmarkMixin {
    @Shadow private boolean[] finished;
    @Shadow private long[] results;
    @Shadow private long[] accumulators;
    @Shadow private int[] totals;

    @Inject(method = "isFinished(I)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$skipSynchronousTiming(int type, CallbackInfoReturnable<Boolean> cir) {
        if (XaeroPerformanceConfig.skipUploadBenchmark()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getAverage", at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$unmeasuredAverage(int type, CallbackInfoReturnable<Long> cir) {
        if (XaeroPerformanceConfig.skipUploadBenchmark()) {
            // Keep real measurements when toggled at runtime. Never divide by
            // zero for an upload type that has deliberately not been sampled.
            cir.setReturnValue(finished[type] ? results[type]
                    : totals[type] > 0 ? accumulators[type] / totals[type]
                    : type == 3 ? 3_000_000L : type == 4 ? 4_000_000L : 1_000_000L);
        }
    }
}
