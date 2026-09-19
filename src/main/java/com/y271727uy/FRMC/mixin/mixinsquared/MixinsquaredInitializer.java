package com.y271727uy.FRMC.mixin.mixinsquared;

import com.y271727uy.FRMC.mixin.mixinsquared.braziliandelight.BrazilianDelightCookingPotMixinSquared;
import com.y271727uy.FRMC.mixin.mixinsquared.manors_bounty.PoisonEffectMixinSquared;
import com.y271727uy.FRMC.mixin.mixinsquared.modernui.MixinWindowResourcesGuard;
import com.y271727uy.FRMC.mixin.mixinsquared.oculus.MixinProgramCancellableGuard;

public final class MixinsquaredInitializer {
    private MixinsquaredInitializer() {
    }

    public static void initialize() {
        PoisonEffectMixinSquared.register();
        MixinWindowResourcesGuard.register();
        BrazilianDelightCookingPotMixinSquared.register();
        MixinProgramCancellableGuard.register();
    }
}
