/*
 * Ported from BiomeSpy 1.3.3 for Minecraft 1.20.1.
 * Copyright (c) 2025 MoePus
 *
 * This file is licensed under the GNU Lesser General Public License, version 3 or later.
 * See third_party/BiomeSpy-NOTICE.txt and third_party/BiomeSpy-LICENSE.txt.
 *
 * Modified for FRMC: package names, mixin wiring and the removals listed in the NOTICE.
 */
package com.y271727uy.FRMC.mixin.terrablender;

import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.integration.terrablender.locate.IParameterListExtendedInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import terrablender.api.RegionType;
import terrablender.util.LevelUtils;

@Mixin(value = LevelUtils.class, remap = false)
public abstract class LevelUtilsMixin {
    @Inject(
            method = "initializeBiomes",
            at = @At(
                    value = "INVOKE",
                    target = "Lterrablender/worldgen/IExtendedParameterList;initializeForTerraBlender(Lnet/minecraft/core/RegistryAccess;Lterrablender/api/RegionType;J)V",
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private static void afterInitializeForTerraBlender(
            RegistryAccess registryAccess,
            Holder<DimensionType> dimensionType,
            ResourceKey<LevelStem> levelResourceKey,
            ChunkGenerator chunkGenerator,
            long seed, CallbackInfo ci,
            @Local RegionType regionType,
            @Local Climate.ParameterList parameters
    ) {
        ((IParameterListExtendedInfo)parameters).biomeSpy$gatherExtendedInfo(registryAccess, regionType, seed);
    }
}
