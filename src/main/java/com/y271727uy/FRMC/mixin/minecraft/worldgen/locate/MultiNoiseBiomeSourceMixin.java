/*
 * Ported from BiomeSpy 1.3.3 for Minecraft 1.20.1.
 * Copyright (c) 2025 MoePus
 *
 * This file is licensed under the GNU Lesser General Public License, version 3 or later.
 * See third_party/BiomeSpy-NOTICE.txt and third_party/BiomeSpy-LICENSE.txt.
 *
 * Modified for FRMC: package names, mixin wiring and the removals listed in the NOTICE.
 */
package com.y271727uy.FRMC.mixin.minecraft.worldgen.locate;

import com.google.common.collect.Sets;
import com.y271727uy.FRMC.capability.worldgen.locate.BiomeFinder;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Set;
import java.util.function.Predicate;

@Mixin(value = MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin extends BiomeSource {
    @Override
    public Pair<BlockPos, Holder<Biome>> findClosestBiome3d(BlockPos pos, int radius, int horizontalStep,
                                                            int verticalStep, Predicate<Holder<Biome>> biomePredicate,
                                                            Climate.Sampler sampler, LevelReader level) {
        return BiomeFinder.findClosestBiome3d(
                pos, radius, horizontalStep, verticalStep, biomePredicate, sampler, level, (MultiNoiseBiomeSource) (Object) this);
    }

// Conflict with an unknown mod in ATM9
//    // Downscaled version of getBiomesWithin for better performance
//    @Override
//    public Set<Holder<Biome>> getBiomesWithin(int x, int y, int z, int radius, Climate.Sampler sampler) {
//        int xMin = QuartPos.fromBlock(x - radius);
//        int yMin = QuartPos.fromBlock(y - radius);
//        int zMin = QuartPos.fromBlock(z - radius);
//        int xMax = QuartPos.fromBlock(x + radius);
//        int yMax = QuartPos.fromBlock(y + radius);
//        int zMax = QuartPos.fromBlock(z + radius);
//
//        int yStep = ((radius <= 4) ? (yMax - yMin) : (yMax - yMin) / 2) + 1;
//
//        Set<Holder<Biome>> set = Sets.newHashSet();
//
//        for (int iX = xMin; iX <= xMax; iX += 2) {
//            for (int iZ = zMin; iZ <= zMax; iZ += 2) {
//                for (int iY = yMin; iY <= yMax; iY += yStep) {
//                    set.add(this.getNoiseBiome(iX, iY, iZ, sampler));
//                }
//            }
//        }
//
//        return set;
//    }
}