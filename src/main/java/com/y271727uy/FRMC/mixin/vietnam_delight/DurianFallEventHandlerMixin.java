package com.y271727uy.FRMC.mixin.vietnam_delight;

import net.huwng.vietnamdelight.DurianFallEventHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = DurianFallEventHandler.class, remap = false)
public abstract class DurianFallEventHandlerMixin {
    @Inject(method = "onLevelTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void frmc$runEveryFiveTicks(TickEvent.LevelTickEvent event, CallbackInfo ci) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.level instanceof ServerLevel level) || level.getGameTime() % 5 != 0) {
            ci.cancel();
        }
    }
}
