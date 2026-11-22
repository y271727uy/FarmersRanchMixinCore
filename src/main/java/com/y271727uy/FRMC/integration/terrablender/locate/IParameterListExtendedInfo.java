/*
 * Ported from BiomeSpy 1.3.3 for Minecraft 1.20.1.
 * Copyright (c) 2025 MoePus
 *
 * This file is licensed under the GNU Lesser General Public License, version 3 or later.
 * See third_party/BiomeSpy-NOTICE.txt and third_party/BiomeSpy-LICENSE.txt.
 *
 * Modified for FRMC: package names, mixin wiring and the removals listed in the NOTICE.
 */
package com.y271727uy.FRMC.integration.terrablender.locate;

import com.y271727uy.FRMC.capability.worldgen.locate.BiomeEnvelope;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import terrablender.api.RegionType;

import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

public interface IParameterListExtendedInfo {
    void biomeSpy$gatherExtendedInfo(RegistryAccess registryAccess, RegionType regionType, long seed);

    BiomeEnvelope biomeSpy$getEnvelopeForBiomes(List<Holder<Biome>> biomes, int regionIndex);

    void biomeSpy$visitAllEnvelopes(BiConsumer<Integer, HashMap<Holder<Biome>, BiomeEnvelope>> visitor);

    boolean biomeSpy$terraBlenderEnabled();
}
