package com.y271727uy.FRMC.mixin.farmersrespite.client;

import com.y271727uy.FRMC.client.farmersrespite.FRMCKettleScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import umpaz.farmersrespite.client.FRClientSetup;
import umpaz.farmersrespite.common.registry.FRMenuTypes;

@Mixin(value = FRClientSetup.class, remap = false)
public abstract class FRClientSetupMixin {
    @Inject(method = "lambda$init$1", at = @At("HEAD"), cancellable = true)
    private static void frmc$registerKettleScreen(CallbackInfo ci) {
        MenuScreens.register(FRMenuTypes.KETTLE.get(), FRMCKettleScreen::new);
        ci.cancel();
    }
}
