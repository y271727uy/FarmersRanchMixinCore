package com.y271727uy.FRMC.bootstrap;

import com.y271727uy.FRMC.mixin.mixinsquared.braziliandelight.BrazilianDelightCookingPotMixinSquared;
import com.y271727uy.FRMC.mixin.mixinsquared.delighto_flight.VillagerComfortMixinSquared;
import com.y271727uy.FRMC.mixin.mixinsquared.manors_bounty.PoisonEffectMixinSquared;
import com.y271727uy.FRMC.mixin.mixinsquared.modernui.MixinWindowResourcesGuard;
import com.y271727uy.FRMC.mixin.mixinsquared.oculus.MixinProgramCancellableGuard;
import com.y271727uy.FRMC.mixin.mixinsquared.acceleratedrendering.GeoRendererMixinSquared;
import com.y271727uy.frmc.mixinfabricextras.MixinFabricExtras;

public final class MixinsquaredBootstrap {
    private MixinsquaredBootstrap() {
    }

    public static void initialize() {
        VillagerComfortMixinSquared.register();
        MixinFabricExtras.init();
        PoisonEffectMixinSquared.register();
        MixinWindowResourcesGuard.register();
        BrazilianDelightCookingPotMixinSquared.register();
        MixinProgramCancellableGuard.register();
        GeoRendererMixinSquared.register();
    }
}
