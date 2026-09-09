package com.y271727uy.FRMC.mixin.manors_bounty.worldgen;

import net.mcreator.manors_bounty.init.ManorsBountyModBiomes;
import net.minecraft.core.Holder;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(value = ManorsBountyModBiomes.class, remap = false)
public abstract class ManorsBountyModBiomesMixin {
    @Unique
    private static volatile RuleSource frmc$cachedInputRule;
    @Unique
    private static RuleSource frmc$cachedAdaptedRule;

    @Shadow
    private static RuleSource injectOverworldSurfaceRules(RuleSource currentRuleSource) {
        throw new AssertionError();
    }

    /**
     * @author FRMC
     * @reason The mod's NoiseGeneratorSettings mixin calls this getter path during chunk surface generation.
     * Cache the immutable composed rule instead of rebuilding its lists and rule tree for every chunk.
     */
    @Overwrite(remap = false)
    public static RuleSource adaptSurfaceRule(RuleSource currentRuleSource, Holder<DimensionType> dimensionType) {
        if (!dimensionType.is(BuiltinDimensionTypes.OVERWORLD)) {
            return currentRuleSource;
        }

        if (currentRuleSource == frmc$cachedInputRule) {
            return frmc$cachedAdaptedRule;
        }

        synchronized (ManorsBountyModBiomesMixin.class) {
            if (currentRuleSource != frmc$cachedInputRule) {
                RuleSource adaptedRule = injectOverworldSurfaceRules(currentRuleSource);
                frmc$cachedAdaptedRule = adaptedRule;
                frmc$cachedInputRule = currentRuleSource;
            }
            return frmc$cachedAdaptedRule;
        }
    }
}
