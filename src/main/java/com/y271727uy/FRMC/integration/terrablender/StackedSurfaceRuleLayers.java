package com.y271727uy.FRMC.integration.terrablender;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import terrablender.worldgen.surface.NamespacedSurfaceRuleSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TerraBlender 3.0.1.10 wraps a noise setting's surface rule in a fresh {@link NamespacedSurfaceRuleSource}
 * every time the setting is read. When the setting is stored inline (custom or modded dimensions), the
 * wrapped rule is what gets saved, so each world load stacks one more layer of every namespace's rules.
 *
 * <p>A layer only falls through to its base when its namespace rule returned {@code null}. If a deeper
 * layer has the same sources as a shallower one, reaching it means the identical rules already returned
 * {@code null} for this position and context, so evaluating it again cannot produce anything else.
 * Dropping such a repeat therefore leaves every surface block unchanged. Distinct older layers are kept.
 */
public final class StackedSurfaceRuleLayers {
    private static final Logger LOGGER = LoggerFactory.getLogger("FRMC-TerraBlender");
    private static final Codec<Map<String, SurfaceRules.RuleSource>> SOURCES_CODEC =
        Codec.unboundedMap(Codec.STRING, SurfaceRules.RuleSource.CODEC);

    private StackedSurfaceRuleLayers() {
    }

    public static SurfaceRules.RuleSource dedupe(SurfaceRules.RuleSource built) {
        try {
            return dedupeOrKeep(built);
        } catch (Throwable throwable) {
            LOGGER.warn("Left TerraBlender surface rule layers unchanged: {}", throwable.toString());
            return built;
        }
    }

    private static SurfaceRules.RuleSource dedupeOrKeep(SurfaceRules.RuleSource built) {
        if (!(built instanceof NamespacedSurfaceRuleSource outer)
            || !(outer.base() instanceof NamespacedSurfaceRuleSource)) {
            return built;
        }

        List<NamespacedSurfaceRuleSource> kept = new ArrayList<>();
        List<Map<String, SurfaceRules.RuleSource>> keptSources = new ArrayList<>();
        List<JsonElement> keptEncoded = new ArrayList<>();
        SurfaceRules.RuleSource node = built;
        int total = 0;
        while (node instanceof NamespacedSurfaceRuleSource layer) {
            total++;
            Map<String, SurfaceRules.RuleSource> sources = layer.sources();
            if (!containsSameInstance(keptSources, sources)) {
                // Any rule that cannot be encoded makes equality unprovable, so keep the whole tree.
                JsonElement encoded = SOURCES_CODEC.encodeStart(JsonOps.INSTANCE, sources).result().orElse(null);
                if (encoded == null) {
                    return built;
                }
                if (!keptEncoded.contains(encoded)) {
                    kept.add(layer);
                    keptSources.add(sources);
                    keptEncoded.add(encoded);
                }
            }
            node = layer.base();
        }

        if (kept.size() == total) {
            return built;
        }
        // node is now the innermost non-namespaced rule; rebuild outward in the original order.
        for (int i = kept.size() - 1; i >= 0; i--) {
            node = new NamespacedSurfaceRuleSource(node, kept.get(i).sources());
        }
        LOGGER.info("TerraBlender surface rules were stacked {} layers deep by earlier world loads; kept {} distinct layers",
            total, kept.size());
        return node;
    }

    private static boolean containsSameInstance(List<Map<String, SurfaceRules.RuleSource>> seen,
                                                Map<String, SurfaceRules.RuleSource> sources) {
        for (Map<String, SurfaceRules.RuleSource> candidate : seen) {
            if (candidate == sources) {
                return true;
            }
        }
        return false;
    }
}
