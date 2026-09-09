package com.y271727uy.FRMC.entity.manager.districtpressure;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared per-chunk Mob snapshot built from the existing periodic activity pass. */
public final class ChunkMobPressureTracker {
    private static final Map<ServerLevel, Map<Long, ChunkPressure>> PRESSURES = new HashMap<>();

    private ChunkMobPressureTracker() {
    }

    public static void refresh(ServerLevel level, Set<Mob> mobs) {
        Map<Long, ChunkPressure> pressures = new HashMap<>();
        for (Mob mob : mobs) {
            if (mob.isRemoved() || mob.level() != level) {
                continue;
            }
            pressures.computeIfAbsent(mob.chunkPosition().toLong(), ChunkPressure::new).add(mob);
        }
        PRESSURES.put(level, pressures);
    }

    public static int totalMobs(ServerLevel level, long chunkPos) {
        ChunkPressure pressure = PRESSURES.getOrDefault(level, Map.of()).get(chunkPos);
        return pressure == null ? 0 : pressure.totalMobs();
    }

    public static Collection<ChunkPressure> pressures(ServerLevel level) {
        return PRESSURES.getOrDefault(level, Map.of()).values();
    }

    public static boolean isTargetModMob(Mob mob) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (id == null) {
            return false;
        }
        return switch (id.getNamespace()) {
            case "alexsmobs", "aquaculture", "unusualfishmod", "untamedwilds" -> true;
            default -> false;
        };
    }

    public static final class ChunkPressure {
        private final long chunkPos;
        private final List<Mob> mobs = new ArrayList<>();
        private int targetModMobs;
        private int nonTargetHostileMobs;

        private ChunkPressure(long chunkPos) {
            this.chunkPos = chunkPos;
        }

        private void add(Mob mob) {
            mobs.add(mob);
            if (isTargetModMob(mob)) {
                targetModMobs++;
            } else if (mob.getType().getCategory() == MobCategory.MONSTER) {
                nonTargetHostileMobs++;
            }
        }

        public long chunkPos() {
            return chunkPos;
        }

        public int totalMobs() {
            return mobs.size();
        }

        public int targetModMobs() {
            return targetModMobs;
        }

        public int nonTargetHostileMobs() {
            return nonTargetHostileMobs;
        }

        public List<Mob> mobs() {
            return mobs;
        }
    }
}
