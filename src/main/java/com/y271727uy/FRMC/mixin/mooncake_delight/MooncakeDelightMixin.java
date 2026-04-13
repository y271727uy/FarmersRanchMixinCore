package com.y271727uy.FRMC.mixin.mooncake_delight;

import com.y271727uy.FRMC.compat.MooncakeDelightCroptopiaCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.chinaex123.mooncake_delight.MooncakeDelight", remap = false)
public abstract class MooncakeDelightMixin {
    @Unique
    private static boolean frmc$attemptedCroptopiaCompatAttach;

    @Inject(method = "<init>", at = @At("RETURN"), require = 0, expect = 0)
    private void frmc$forceCroptopiaCompatRegistration(CallbackInfo ci) {
        if (frmc$attemptedCroptopiaCompatAttach) {
            return;
        }

        frmc$attemptedCroptopiaCompatAttach = true;
        MooncakeDelightCroptopiaCompat.forceRegisterCroptopiaCompat();
    }
}

