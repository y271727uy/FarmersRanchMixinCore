package com.y271727uy.FRMC.mixin.terrablender;

import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(targets = "terrablender.worldgen.surface.NamespacedSurfaceRuleSource$NamespacedRule", remap = false)
public interface NamespacedRuleAccessor {
    @Accessor("baseRule")
    SurfaceRules.SurfaceRule frmc$getBaseRule();

    @Accessor("rules")
    Map<String, SurfaceRules.SurfaceRule> frmc$getRules();
}
