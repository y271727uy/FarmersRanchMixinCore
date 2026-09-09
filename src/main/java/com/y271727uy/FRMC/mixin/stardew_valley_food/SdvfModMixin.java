package com.y271727uy.FRMC.mixin.stardew_valley_food;

import net.minecraftforge.event.entity.player.PlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses Stardew Valley Food's optional-mod reminder sent when a player logs in. */
@Mixin(targets = "th.tamkungz.sdvf.SdvfMod", remap = false)
@Pseudo
public abstract class SdvfModMixin {
    @Inject(method = "onPlayerLogin", at = @At("HEAD"), cancellable = true, remap = false)
    private void frmc$suppressOptionalModReminder(PlayerEvent.PlayerLoggedInEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
