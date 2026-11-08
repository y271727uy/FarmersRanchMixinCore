package com.y271727uy.FRMC.mixin.terrablender;

import com.y271727uy.FRMC.integration.terrablender.AreaCacheDiagnostics;
import com.y271727uy.FRMC.integration.terrablender.CachedNamespacedSurfaceRule;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import terrablender.worldgen.surface.NamespacedSurfaceRuleSource;

@Mixin(value = NamespacedSurfaceRuleSource.class, remap = false)
public abstract class NamespacedSurfaceRuleSourceMixin {
    @Inject(method = "apply(Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;)Lnet/minecraft/world/level/levelgen/SurfaceRules$SurfaceRule;",
        at = @At("RETURN"), cancellable = true)
    private void frmc$cacheDispatch(SurfaceRules.Context context, CallbackInfoReturnable<SurfaceRules.SurfaceRule> cir) {
        if (AreaCacheDiagnostics.cacheSurfaceRuleDispatch() && cir.getReturnValue() instanceof NamespacedRuleAccessor rule) {
            cir.setReturnValue(new CachedNamespacedSurfaceRule(context, rule.frmc$getBaseRule(), rule.frmc$getRules(),
                AreaCacheDiagnostics.cacheSecondSurfaceBiome()));
        }
    }
}
