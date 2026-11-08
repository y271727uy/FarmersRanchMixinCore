package com.y271727uy.FRMC.integration.terrablender;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.Map;

/** Caches dispatch for one surface context, whose lifetime is one surface build. */
public final class CachedNamespacedSurfaceRule implements SurfaceRules.SurfaceRule {
    private final SurfaceRules.Context context;
    private final SurfaceRules.SurfaceRule baseRule;
    private final Map<String, SurfaceRules.SurfaceRule> rules;
    private final boolean cacheSecondBiome;
    private final boolean sampleDispatch;
    private final SurfaceDispatchGuard dispatchGuard;
    private Holder<Biome> lastBiome;
    private SurfaceRules.SurfaceRule lastRule;
    private Holder<Biome> previousBiome;
    private SurfaceRules.SurfaceRule previousRule;

    public CachedNamespacedSurfaceRule(SurfaceRules.Context context, SurfaceRules.SurfaceRule baseRule,
                                       Map<String, SurfaceRules.SurfaceRule> rules, boolean cacheSecondBiome) {
        this.context = context;
        this.baseRule = baseRule;
        this.rules = rules;
        this.cacheSecondBiome = cacheSecondBiome;
        this.sampleDispatch = AreaCacheDiagnostics.shouldSampleSurfaceDispatch();
        this.dispatchGuard = AreaCacheDiagnostics.skipInactiveSurfaceDispatch() ? SurfaceDispatchGuard.create(rules.values()) : null;
        if (sampleDispatch && AreaCacheDiagnostics.skipInactiveSurfaceDispatch()) {
            AreaCacheDiagnostics.recordSurfaceGuardContext(dispatchGuard != null);
        }
    }

    @Override
    public BlockState tryApply(int x, int y, int z) {
        if (dispatchGuard != null && dispatchGuard.canSkipDispatch()) {
            if (sampleDispatch) {
                AreaCacheDiagnostics.recordSurfaceDispatchBypass();
            }
            return baseRule.tryApply(x, y, z);
        }
        Holder<Biome> biome = context.biome.get();
        SurfaceRules.SurfaceRule selected = selectRule(biome);
        BlockState state = selected == null ? null : selected.tryApply(x, y, z);
        return state == null ? baseRule.tryApply(x, y, z) : state;
    }

    private SurfaceRules.SurfaceRule selectRule(Holder<Biome> biome) {
        if (biome != null && biome == lastBiome) {
            if (sampleDispatch) {
                AreaCacheDiagnostics.recordFirstSurfaceHit();
            }
            return lastRule;
        }
        if (cacheSecondBiome && biome != null && biome == previousBiome) {
            if (sampleDispatch) {
                AreaCacheDiagnostics.recordSecondSurfaceHit();
            }
            return remember(biome, previousRule);
        }
        if (biome instanceof Holder.Reference<Biome> reference && biome.getClass() == Holder.Reference.class && reference.isBound()) {
            if (sampleDispatch) {
                AreaCacheDiagnostics.recordSurfaceNamespaceLookup();
            }
            return remember(biome, rules.get(reference.key().location().getNamespace()));
        }
        if (sampleDispatch) {
            AreaCacheDiagnostics.recordUncachedSurfaceHolder();
        }
        if (biome instanceof Holder.Direct<?>) {
            return null;
        }
        if (biome.is(key -> rules.containsKey(key.location().getNamespace()))) {
            return rules.get(biome.unwrapKey().get().location().getNamespace());
        }
        return null;
    }

    private SurfaceRules.SurfaceRule remember(Holder<Biome> biome, SurfaceRules.SurfaceRule rule) {
        if (cacheSecondBiome) {
            previousBiome = lastBiome;
            previousRule = lastRule;
        }
        lastBiome = biome;
        lastRule = rule;
        return rule;
    }
}
