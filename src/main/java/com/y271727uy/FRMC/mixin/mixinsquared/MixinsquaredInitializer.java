package com.y271727uy.FRMC.mixin.mixinsquared;

import com.y271727uy.FRMC.mixin.mixinsquared.manors_bounty.PoisonEffectMixinSquared;
import com.y271727uy.FRMC.mixin.mixinsquared.modernui.MixinWindowResourcesGuard;

public final class MixinsquaredInitializer {
    private MixinsquaredInitializer() {
    }

    public static void initialize() {
        PoisonEffectMixinSquared.register();
        MixinWindowResourcesGuard.register();
    }
}
