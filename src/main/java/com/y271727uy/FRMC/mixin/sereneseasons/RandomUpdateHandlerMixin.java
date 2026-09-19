package com.y271727uy.FRMC.mixin.sereneseasons;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "sereneseasons.season.RandomUpdateHandler", remap = false)
public abstract class RandomUpdateHandlerMixin {
    @Inject(
        method = "onWorldTick",
        at = @At(
            value = "INVOKE",
            target = "Lsereneseasons/season/RandomUpdateHandler;adjustWeatherFrequency(Lnet/minecraft/world/level/Level;Lsereneseasons/api/season/Season$SubSeason;)V",
            shift = At.Shift.AFTER,
            remap = false
        ),
        cancellable = true,
        remap = false,
        require = 1
    )
    private static void frmc$skipChunkMapMeltScan(CallbackInfo ci) {
        ci.cancel();
    }
}
