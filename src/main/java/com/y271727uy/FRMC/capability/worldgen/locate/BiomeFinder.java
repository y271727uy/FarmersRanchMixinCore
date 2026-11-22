/*
 * Ported from BiomeSpy 1.3.3 for Minecraft 1.20.1.
 * Copyright (c) 2025 MoePus
 *
 * This file is licensed under the GNU Lesser General Public License, version 3 or later.
 * See third_party/BiomeSpy-NOTICE.txt and third_party/BiomeSpy-LICENSE.txt.
 *
 * Modified for FRMC: package names, mixin wiring and the removals listed in the NOTICE.
 */
package com.y271727uy.FRMC.capability.worldgen.locate;

import com.y271727uy.FRMC.integration.alexscaves.locate.AlexBiome;
import com.y271727uy.FRMC.integration.alexscaves.locate.AlexsCavesCompat;
import com.y271727uy.FRMC.integration.terrablender.locate.TerraBiome;
import com.y271727uy.FRMC.integration.terrablender.locate.TerrablenderCompat;
import com.y271727uy.FRMC.mixin.minecraft.worldgen.locate.MultiNoiseBiomeSourceAccessor;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BiomeFinder {
    public static Pair<BlockPos, Holder<Biome>> findClosestBiome3d(BlockPos pos, int radius, int horizontalStep,
                                                                   int verticalStep, Predicate<Holder<Biome>> biomePredicate,
                                                                   Climate.Sampler sampler, LevelReader level,
                                                                   MultiNoiseBiomeSource biomeSource) {
        Set<Holder<Biome>> set = biomeSource.possibleBiomes().stream().filter(biomePredicate).collect(Collectors.toUnmodifiableSet());
        if (set.isEmpty()) return null;

        var parameters = ((MultiNoiseBiomeSourceAccessor) biomeSource).invokeParameters();
        BiomeEnvelopeSelector envelopeSelector = new BiomeEnvelopeSelector(set, parameters, biomeSource);
        BiomeNoiseCheckState noiseCheckState = new BiomeNoiseCheckState();

        int i = Math.floorDiv(radius * 2, horizontalStep);
        int[] heights = Mth.outFromOrigin(pos.getY(), level.getMinBuildHeight() + 1, level.getMaxBuildHeight(), verticalStep).toArray();
        for (BlockPos.MutableBlockPos blockpos$mutableblockpos : BlockPos.spiralAround(BlockPos.ZERO, i, Direction.EAST, Direction.SOUTH)) {
            int x = pos.getX() + blockpos$mutableblockpos.getX() * horizontalStep;
            int z = pos.getZ() + blockpos$mutableblockpos.getZ() * horizontalStep;
            LazyBiomeNoiseChecker biomeChecker = new LazyBiomeNoiseChecker(envelopeSelector, parameters, x, z);

            for (int y : heights) {
                if (AlexsCavesCompat.ALEXS_CAVES_INSTALLED) {
                    var alexBiome = AlexBiome.getAlexsCavesBiome(envelopeSelector, sampler, x, y, z);
                    if (alexBiome != null) {
                        if (set.contains(alexBiome))
                            return Pair.of(new BlockPos(x, y, z), alexBiome);
                        else
                            continue;
                    }
                }
                if (!biomeChecker.matches(sampler, noiseCheckState, y))
                    continue;
                Climate.TargetPoint climate = biomeChecker.toTargetPoint(sampler, y);
                Holder<Biome> holder;
                if (TerrablenderCompat.TERRABLENDER_INSTALLED) {
                    holder = TerraBiome.getNoiseBiome(parameters, climate, x, y, z);
                } else {
                    holder = biomeSource.getNoiseBiome(climate);
                }
                if (set.contains(holder)) {
                    return Pair.of(new BlockPos(x, y, z), holder);
                }
            }
        }
        return null;
    }
}
