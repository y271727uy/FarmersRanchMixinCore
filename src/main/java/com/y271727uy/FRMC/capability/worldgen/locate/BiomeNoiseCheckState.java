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

public class BiomeNoiseCheckState {
    public enum NoiseType {
        TEMPERATURE, HUMIDITY, WEIRDNESS, CONTINENTALNESS, EROSION, NONE
    }

    private NoiseType lastFilteredNoise = NoiseType.NONE;

    public NoiseType getLastFilteredNoise() {
        return lastFilteredNoise;
    }

    public void setLastFilteredNoise(NoiseType type) {
        this.lastFilteredNoise = type;
    }

    public void clear() {
        this.lastFilteredNoise = NoiseType.NONE;
    }
}
