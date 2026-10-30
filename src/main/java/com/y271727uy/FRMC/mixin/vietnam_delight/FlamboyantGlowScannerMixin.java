package com.y271727uy.FRMC.mixin.vietnam_delight;

import net.minecraft.client.Minecraft;
import net.huwng.vietnamdelight.client.bloom.FlamboyantGlowScanner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = FlamboyantGlowScanner.class, remap = false)
public abstract class FlamboyantGlowScannerMixin {
    @Unique private static int frmc$nearScanTicks;
    @Unique private static int frmc$wideScanTicks;

    @Inject(method = "rescanNear", at = @At("HEAD"), cancellable = true, remap = false)
    private static void frmc$throttleNearScan(Minecraft minecraft, CallbackInfo ci) {
        if (++frmc$nearScanTicks < 5) {
            ci.cancel();
        } else {
            frmc$nearScanTicks = 0;
        }
    }

    @Inject(method = "rescan", at = @At("HEAD"), cancellable = true, remap = false)
    private static void frmc$throttleWideScan(Minecraft minecraft, CallbackInfo ci) {
        if (++frmc$wideScanTicks < 5) {
            ci.cancel();
        } else {
            frmc$wideScanTicks = 0;
        }
    }
}
