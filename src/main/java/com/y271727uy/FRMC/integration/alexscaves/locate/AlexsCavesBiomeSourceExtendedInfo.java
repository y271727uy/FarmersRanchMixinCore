/*
 * Ported from BiomeSpy 1.3.3 for Minecraft 1.20.1.
 * Copyright (c) 2025 MoePus
 *
 * This file is licensed under the GNU Lesser General Public License, version 3 or later.
 * See third_party/BiomeSpy-NOTICE.txt and third_party/BiomeSpy-LICENSE.txt.
 *
 * Modified for FRMC: package names, mixin wiring and the removals listed in the NOTICE.
 */
package com.y271727uy.FRMC.integration.alexscaves.locate;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record AlexsCavesBiomeSourceExtendedInfo(long lastSampledWorldSeed, ResourceKey<Level> lastSampledDimension) {
    public AlexsCavesBiomeSourceExtendedInfo(long lastSampledWorldSeed, ResourceKey<Level> lastSampledDimension) {
        this.lastSampledWorldSeed = lastSampledWorldSeed;
        this.lastSampledDimension = lastSampledDimension;
    }

    public long lastSampledWorldSeed() {
        return this.lastSampledWorldSeed;
    }

    public ResourceKey<Level> lastSampledDimension() {
        return this.lastSampledDimension;
    }
}
