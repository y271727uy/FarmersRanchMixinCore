package com.y271727uy.FRMC.integration.alexscaves;

import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig;
import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationNoiseCondition;
import com.y271727uy.FRMC.mixin.alexscaves.BiomeGenerationNoiseConditionAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alex's Caves hooks every {@code MultiNoiseBiomeSource#getNoiseBiome} call: it builds a Voronoi cell,
 * samples the climate, then asks each cave biome condition whether it applies. A condition returns
 * {@code false} when it is {@code disabled_completely}, and when the sampled dimension is not in its
 * {@code dimensions} list. When no condition can pass either check for a dimension, the hook can
 * never replace the biome there, so skipping it is exact. This covers the Nether and modded
 * dimensions, which the default configs never list.
 */
public final class CaveBiomeHookGate {
    private static final Logger LOGGER = LoggerFactory.getLogger("FRMC-AlexsCaves");
    private static final Map<ResourceKey<Level>, Boolean> SKIP_BY_DIMENSION = new ConcurrentHashMap<>();
    private static volatile Boolean skipWithoutDimension;

    private CaveBiomeHookGate() {
    }

    public static boolean canSkip(MultiNoiseBiomeSource source) {
        ResourceKey<Level> dimension = DimensionField.read(source);
        if (dimension == null) {
            Boolean cached = skipWithoutDimension;
            if (cached == null) {
                cached = compute(null);
                skipWithoutDimension = cached;
            }
            return cached;
        }
        return SKIP_BY_DIMENSION.computeIfAbsent(dimension, CaveBiomeHookGate::compute);
    }

    public static void invalidate() {
        SKIP_BY_DIMENSION.clear();
        skipWithoutDimension = null;
    }

    private static boolean compute(ResourceKey<Level> dimension) {
        String id = dimension == null ? null : dimension.location().toString();
        for (BiomeGenerationNoiseCondition condition : BiomeGenerationConfig.BIOMES.values()) {
            if (condition == null || condition.isDisabledCompletely()) {
                continue;
            }
            List<String> dimensions = ((BiomeGenerationNoiseConditionAccessor) condition).frmc$getDimensions();
            // A null dimension skips the list check in Alex's Caves; an unreadable list is treated as matching.
            if (id == null || dimensions == null || dimensions.contains(id)) {
                return false;
            }
        }
        return true;
    }

    /** Alex's Caves merges a private {@code lastSampledDimension} field into the biome source. */
    private static final class DimensionField {
        private static final MethodHandle GETTER = find();

        private static MethodHandle find() {
            try {
                Field field = MultiNoiseBiomeSource.class.getDeclaredField("lastSampledDimension");
                field.setAccessible(true);
                return MethodHandles.lookup().unreflectGetter(field);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                LOGGER.warn("Alex's Caves lastSampledDimension not found, cave biome hook is only skipped when every cave biome is disabled: {}",
                    exception.toString());
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        static ResourceKey<Level> read(MultiNoiseBiomeSource source) {
            if (GETTER == null) {
                return null;
            }
            try {
                return (ResourceKey<Level>) GETTER.invoke(source);
            } catch (Throwable throwable) {
                return null;
            }
        }
    }
}
