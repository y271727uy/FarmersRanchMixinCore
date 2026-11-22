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

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.worldgen.IExtendedParameterList;

public class TerraBiome {
    public static Holder<Biome> getNoiseBiome(Climate.ParameterList<Holder<Biome>> parameters, Climate.TargetPoint climate, int x, int y, int z) {
        return (Holder<Biome>)(((IExtendedParameterList) parameters).
                findValuePositional(climate, QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)));
    }

    public static int getUniqueness(Climate.ParameterList<Holder<Biome>> parameters, int qx, int qy, int qz) {
        return ((IExtendedParameterList<?>) parameters).getUniqueness(qx, qy, qz);
    }
}
