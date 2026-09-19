package com.y271727uy.FRMC.capability.blockchecking.server;

import com.y271727uy.FRMC.capability.entity.manager.districtpressure.ChunkMobPressureTracker;
import com.y271727uy.FRMC.capability.entity.manager.entityactivity.EntityActivityManager;
import com.y271727uy.FRMC.capability.blockchecking.aggregate.ChunkPressureOrderer;
import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureEntry;
import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureSnapshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Copies primitive pressure counts after refreshing the tracker from already-tracked mobs. */
public final class ChunkPressureSnapshotCollector {
    public ChunkPressureSnapshot collect(MinecraftServer server, int limit) {
        EntityActivityManager.refreshPressureNow();
        List<ChunkPressureEntry> copied = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            String dimension = level.dimension().location().toString();
            Map<Long, ChunkMobPressureTracker.ChunkPressure> byChunk = new HashMap<>();
            for (ChunkMobPressureTracker.ChunkPressure pressure : ChunkMobPressureTracker.pressures(level)) {
                byChunk.put(pressure.chunkPos(), pressure);
            }
            Map<Long, EntityCounts> entitiesByChunk = entityCounts(level);
            Map<Long, Integer> scheduledByChunk = scheduledTickCounts(level);
            List<ServerPlayer> players = EntityActivityManager.activePlayers(level);
            Set<Long> packedChunks = new HashSet<>(byChunk.keySet());
            for (Map.Entry<Long, EntityCounts> entities : entitiesByChunk.entrySet()) {
                if (entities.getValue().items > 0) {
                    packedChunks.add(entities.getKey());
                }
            }
            packedChunks.addAll(scheduledByChunk.keySet());
            for (long packed : packedChunks) {
                EntityCounts entities = entitiesByChunk.getOrDefault(packed, EntityCounts.EMPTY);
                copied.add(entry(level, dimension, packed, byChunk.get(packed), entities.items,
                        scheduledByChunk.getOrDefault(packed, 0), entities.nonMobs, players));
            }
        }
        return new ChunkPressureSnapshot(System.currentTimeMillis(), ChunkPressureOrderer.sortAndLimit(copied, limit), true);
    }

    /**
     * Always one row for a loaded dimension, even when the tracker has never seen that chunk.
     * Unknown dimension yields an empty snapshot.
     */
    public ChunkPressureSnapshot collectOne(MinecraftServer server, String dimension, long packedChunk) {
        EntityActivityManager.refreshPressureNow();
        ServerLevel level = findLevel(server, dimension);
        if (level == null) {
            return new ChunkPressureSnapshot(System.currentTimeMillis(), List.of(), true);
        }
        return new ChunkPressureSnapshot(System.currentTimeMillis(),
                List.of(entry(level, dimension, packedChunk, findPressure(level, packedChunk),
                        itemEntities(level, packedChunk), scheduledTicks(level, packedChunk),
                        nonMobEntities(level, packedChunk), EntityActivityManager.activePlayers(level))), true);
    }

    private static ChunkPressureEntry entry(ServerLevel level, String dimension, long packedChunk,
                                            ChunkMobPressureTracker.ChunkPressure pressure, int itemEntities,
                                            int scheduledTicks, int nonMobEntities,
                                            List<ServerPlayer> players) {
        int trackedMobs = 0;
        int targetModMobs = 0;
        int otherHostileMobs = 0;
        int sleepingMobs = 0;
        int throttledMobs = 0;
        int pathfindingMobs = 0;
        int pendingThrottleMobs = 0;
        int pendingSleepMobs = 0;
        if (pressure != null) {
            trackedMobs = pressure.totalMobs();
            targetModMobs = pressure.targetModMobs();
            otherHostileMobs = pressure.nonTargetHostileMobs();
            for (Mob mob : pressure.mobs()) {
                if (EntityActivityManager.isSleeping(mob)) {
                    sleepingMobs++;
                } else if (EntityActivityManager.isThrottled(mob)) {
                    throttledMobs++;
                } else if (EntityActivityManager.isPendingThrottle(mob, players)) {
                    pendingThrottleMobs++;
                } else if (EntityActivityManager.isPendingSleep(mob, players)) {
                    pendingSleepMobs++;
                }
                if (isPathfinding(mob)) {
                    pathfindingMobs++;
                }
            }
        }
        return new ChunkPressureEntry(dimension, packedChunk, trackedMobs, targetModMobs, otherHostileMobs,
                tickingBlockEntities(level, packedChunk), sleepingMobs, throttledMobs,
                StarlightChunkLightProbe.queuedTasks(level, packedChunk), itemEntities, scheduledTicks,
                nonMobEntities, pathfindingMobs, pendingThrottleMobs, pendingSleepMobs);
    }

    private static boolean isPathfinding(Mob mob) {
        var navigation = mob.getNavigation();
        return navigation != null && navigation.isInProgress();
    }

    private static ServerLevel findLevel(MinecraftServer server, String dimension) {
        if (server == null || dimension == null || dimension.isEmpty()) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (dimension.equals(level.dimension().location().toString())) {
                return level;
            }
        }
        return null;
    }

    private static ChunkMobPressureTracker.ChunkPressure findPressure(ServerLevel level, long packedChunk) {
        for (ChunkMobPressureTracker.ChunkPressure pressure : ChunkMobPressureTracker.pressures(level)) {
            if (pressure.chunkPos() == packedChunk) {
                return pressure;
            }
        }
        return null;
    }

    private static int tickingBlockEntities(ServerLevel level, long packedChunk) {
        int chunkX = ChunkPos.getX(packedChunk);
        int chunkZ = ChunkPos.getZ(packedChunk);
        if (!level.hasChunk(chunkX, chunkZ)) {
            return 0;
        }
        LevelChunk chunk = level.getChunk(chunkX, chunkZ);
        return chunk.getBlockEntities().size();
    }

    private static Map<Long, Integer> scheduledTickCounts(ServerLevel level) {
        Map<Long, Integer> counts = new HashMap<>();
        for (ChunkHolder holder : level.getChunkSource().chunkMap.getChunks()) {
            LevelChunk chunk = holder.getTickingChunk();
            if (chunk == null) {
                continue;
            }
            int ticks = scheduledTicks(chunk);
            if (ticks > 0) {
                counts.put(chunk.getPos().toLong(), ticks);
            }
        }
        return counts;
    }

    private static int scheduledTicks(ServerLevel level, long packedChunk) {
        int chunkX = ChunkPos.getX(packedChunk);
        int chunkZ = ChunkPos.getZ(packedChunk);
        if (!level.hasChunk(chunkX, chunkZ)) {
            return 0;
        }
        return scheduledTicks(level.getChunk(chunkX, chunkZ));
    }

    private static int scheduledTicks(LevelChunk chunk) {
        return Math.max(0, chunk.getBlockTicks().count()) + Math.max(0, chunk.getFluidTicks().count());
    }

    private static Map<Long, EntityCounts> entityCounts(ServerLevel level) {
        Map<Long, EntityCounts> counts = new HashMap<>();
        for (Entity entity : level.getAllEntities()) {
            if (!entity.isAlive() || entity instanceof Player) {
                continue;
            }
            long packed = entity.chunkPosition().toLong();
            if (entity instanceof ItemEntity) {
                counts.computeIfAbsent(packed, ignored -> new EntityCounts()).items++;
            } else if (!(entity instanceof Mob)) {
                counts.computeIfAbsent(packed, ignored -> new EntityCounts()).nonMobs++;
            }
        }
        return counts;
    }

    private static int itemEntities(ServerLevel level, long packedChunk) {
        return countEntities(level, packedChunk, true);
    }

    private static int nonMobEntities(ServerLevel level, long packedChunk) {
        return countEntities(level, packedChunk, false);
    }

    private static int countEntities(ServerLevel level, long packedChunk, boolean items) {
        int chunkX = ChunkPos.getX(packedChunk);
        int chunkZ = ChunkPos.getZ(packedChunk);
        if (!level.hasChunk(chunkX, chunkZ)) {
            return 0;
        }
        AABB box = new AABB(chunkX * 16 - 1, level.getMinBuildHeight(), chunkZ * 16 - 1,
                chunkX * 16 + 17, level.getMaxBuildHeight(), chunkZ * 16 + 17);
        int[] count = {0};
        level.getEntities().get(box, entity -> {
            if (!entity.isAlive() || entity.chunkPosition().toLong() != packedChunk || entity instanceof Player) {
                return;
            }
            if (items) {
                if (entity instanceof ItemEntity) {
                    count[0]++;
                }
            } else if (!(entity instanceof Mob) && !(entity instanceof ItemEntity)) {
                count[0]++;
            }
        });
        return count[0];
    }

    private static final class EntityCounts {
        private static final EntityCounts EMPTY = new EntityCounts();
        private int items;
        private int nonMobs;
    }
}
