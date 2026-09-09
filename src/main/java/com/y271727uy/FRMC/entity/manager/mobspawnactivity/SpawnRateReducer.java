package com.y271727uy.FRMC.entity.manager.mobspawnactivity;

import net.minecraft.world.level.biome.MobSpawnSettings;

/** Creates a lower-weight copy while preserving the source Mod's group-size rules. */
public final class SpawnRateReducer {
    private SpawnRateReducer() {
    }

    public static MobSpawnSettings.SpawnerData reduceWeight(MobSpawnSettings.SpawnerData spawn, double multiplier) {
        int weight = Math.max(1, (int) Math.floor(spawn.getWeight().asInt() * multiplier));
        if (weight == spawn.getWeight().asInt()) {
            return spawn;
        }
        return new MobSpawnSettings.SpawnerData(spawn.type, weight, spawn.minCount, spawn.maxCount);
    }
}
