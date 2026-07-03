package com.y271727uy.FRMC.mixin.citadel;

import com.github.alexthe666.citadel.web.WebHelper;
import java.io.BufferedReader;
import java.io.StringReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WebHelper.class, remap = false)
public abstract class WebHelperMixin {
    @Inject(method = "getURLContents", at = @At("HEAD"), cancellable = true)
    private static void frmc$disableCitadelWebRequests(String url, String fallbackResource, CallbackInfoReturnable<BufferedReader> cir) {
        cir.setReturnValue(new BufferedReader(new StringReader("")));
    }
}
