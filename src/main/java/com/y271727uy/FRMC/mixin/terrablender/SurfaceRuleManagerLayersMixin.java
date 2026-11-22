package com.y271727uy.FRMC.mixin.terrablender;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.y271727uy.FRMC.integration.terrablender.AreaCacheDiagnostics;
import com.y271727uy.FRMC.integration.terrablender.StackedSurfaceRuleLayers;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import terrablender.api.SurfaceRuleManager;

@Mixin(value = SurfaceRuleManager.class, remap = false)
public abstract class SurfaceRuleManagerLayersMixin {
    @ModifyReturnValue(method = "getNamespacedRules", at = @At("RETURN"))
    private static SurfaceRules.RuleSource frmc$dropRepeatedLayers(SurfaceRules.RuleSource built) {
        return AreaCacheDiagnostics.dedupeStackedSurfaceRules() ? StackedSurfaceRuleLayers.dedupe(built) : built;
    }
}
