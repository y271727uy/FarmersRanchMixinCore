package com.y271727uy.FRMC.mixin.terrablender;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.y271727uy.FRMC.integration.terrablender.AreaCacheDiagnostics;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SurfaceSystem.class)
public abstract class SurfaceBuildTimingMixin {
    @WrapMethod(method = "buildSurface")
    private void frmc$timeSurfaceBuild(RandomState randomState, BiomeManager biomeManager, Registry<Biome> biomes,
                                       boolean useLegacyRandom, WorldGenerationContext context, ChunkAccess chunk,
                                       NoiseChunk noiseChunk, SurfaceRules.RuleSource rules, Operation<Void> original) {
        boolean timed = AreaCacheDiagnostics.surfaceBuildDiagnostics();
        long start = timed ? System.nanoTime() : 0L;
        original.call(randomState, biomeManager, biomes, useLegacyRandom, context, chunk, noiseChunk, rules);
        if (timed) {
            AreaCacheDiagnostics.recordSurfaceBuild(System.nanoTime() - start);
        }
    }
}
