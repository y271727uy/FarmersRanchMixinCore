package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.client.render.CottonCandyMachineHudLayer;
import com.y271727uy.FRMC.integration.delighto_flight.CottonCandyHudState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = CottonCandyMachineHudLayer.class, remap = false)
public abstract class CottonCandyMachineHudLayerMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void frmc$skipRaycastWithoutLocalSpinner(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !CottonCandyHudState.mayShowProgress(minecraft.level, minecraft.player.getUUID())) {
            ci.cancel();
        }
    }
}
