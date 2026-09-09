package com.y271727uy.FRMC.entity.manager.mobspawnclean;

import com.y271727uy.FRMC.entity.manager.districtpressure.ChunkMobPressureTracker;
import com.y271727uy.FRMC.entity.manager.entityactivity.EntityActivityManager;
import com.y271727uy.FRMC.config.EntityActivityConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.raid.Raider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Removes only safely disposable, hidden Mobs from persistently overloaded distant chunks. */
public final class EntityOverloadCleanupManager {
    private static final long INSPECTION_INTERVAL_TICKS = 200L;
    private static final Map<ServerLevel, Map<Long, Long>> LAST_INSPECTION = new HashMap<>();
    private static final Map<ServerLevel, Map<Long, Long>> LAST_CLEANUP = new HashMap<>();

    private EntityOverloadCleanupManager() {
    }

    public static void evaluate(ServerLevel level, List<ServerPlayer> players) {
        if (!EntityActivityConfig.overloadCleanupEnabled()) {
            return;
        }
        long now = level.getGameTime();
        for (ChunkMobPressureTracker.ChunkPressure pressure : ChunkMobPressureTracker.pressures(level)) {
            if (pressure.totalMobs() < EntityActivityConfig.overloadCleanupPressureThreshold()
                    || recentlyInspected(level, pressure.chunkPos(), now)
                    || recentlyCleaned(level, pressure.chunkPos(), now)) {
                continue;
            }
            markInspected(level, pressure.chunkPos(), now);
            int removed = cleanup(pressure, players);
            if (removed > 0) {
                LAST_CLEANUP.computeIfAbsent(level, ignored -> new HashMap<>()).put(pressure.chunkPos(), now);
            }
        }
    }

    private static int cleanup(ChunkMobPressureTracker.ChunkPressure pressure, List<ServerPlayer> players) {
        List<Mob> targetModCandidates = new ArrayList<>();
        List<Mob> hostileCandidates = new ArrayList<>();
        for (Mob mob : pressure.mobs()) {
            if (!isDisposableHiddenMob(mob, players)) {
                continue;
            }
            if (ChunkMobPressureTracker.isTargetModMob(mob)) {
                targetModCandidates.add(mob);
            } else if (mob.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {
                hostileCandidates.add(mob);
            }
        }
        return discard(targetModCandidates, 0.75D) + discard(hostileCandidates, 0.25D);
    }

    private static boolean isDisposableHiddenMob(Mob mob, List<ServerPlayer> players) {
        return !mob.isRemoved()
                && !mob.isPersistenceRequired()
                && !mob.hasCustomName()
                && !mob.isLeashed()
                && !(mob instanceof OwnableEntity)
                && !(mob instanceof Bucketable bucketable && bucketable.fromBucket())
                && !(mob instanceof Raider)
                && mob.getPassengers().isEmpty()
                && mob.getVehicle() == null
                && EntityActivityManager.isCleanupEligible(mob, players);
    }

    private static int discard(List<Mob> mobs, double fraction) {
        int limit = (int) Math.floor(mobs.size() * fraction);
        for (int index = 0; index < limit; index++) {
            mobs.get(index).discard();
        }
        return limit;
    }

    private static boolean recentlyInspected(ServerLevel level, long chunkPos, long now) {
        Long lastInspection = LAST_INSPECTION.getOrDefault(level, Map.of()).get(chunkPos);
        return lastInspection != null && now - lastInspection < INSPECTION_INTERVAL_TICKS;
    }

    private static boolean recentlyCleaned(ServerLevel level, long chunkPos, long now) {
        Long lastCleanup = LAST_CLEANUP.getOrDefault(level, Map.of()).get(chunkPos);
        return lastCleanup != null && now - lastCleanup < EntityActivityConfig.overloadCleanupCooldownTicks();
    }

    private static void markInspected(ServerLevel level, long chunkPos, long now) {
        LAST_INSPECTION.computeIfAbsent(level, ignored -> new HashMap<>()).put(chunkPos, now);
    }
}
