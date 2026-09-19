package com.y271727uy.FRMC.capability.blockchecking.model;

import java.util.Objects;

/** Immutable primitive-count copy of one tracked-mob chunk pressure row. */
public record ChunkPressureEntry(String dimension, long packedChunk, int trackedMobs, int targetModMobs,
                                 int otherHostileMobs, int tickingBlockEntities, int sleepingMobs,
                                 int throttledMobs, int lightingTasks, int itemEntities, int scheduledTicks,
                                 int nonMobEntities, int pathfindingMobs, int pendingThrottleMobs,
                                 int pendingSleepMobs) {
    public ChunkPressureEntry {
        dimension = Objects.requireNonNull(dimension, "dimension");
        trackedMobs = Math.max(0, trackedMobs);
        targetModMobs = Math.max(0, targetModMobs);
        otherHostileMobs = Math.max(0, otherHostileMobs);
        tickingBlockEntities = Math.max(0, tickingBlockEntities);
        sleepingMobs = Math.max(0, sleepingMobs);
        throttledMobs = Math.max(0, throttledMobs);
        lightingTasks = Math.max(0, lightingTasks);
        itemEntities = Math.max(0, itemEntities);
        scheduledTicks = Math.max(0, scheduledTicks);
        nonMobEntities = Math.max(0, nonMobEntities);
        pathfindingMobs = Math.max(0, pathfindingMobs);
        pendingThrottleMobs = Math.max(0, pendingThrottleMobs);
        pendingSleepMobs = Math.max(0, pendingSleepMobs);
    }

    public ChunkPressureEntry(String dimension, long packedChunk, int trackedMobs, int targetModMobs,
                              int otherHostileMobs) {
        this(dimension, packedChunk, trackedMobs, targetModMobs, otherHostileMobs, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public ChunkPressureEntry(String dimension, long packedChunk, int trackedMobs, int targetModMobs,
                              int otherHostileMobs, int tickingBlockEntities) {
        this(dimension, packedChunk, trackedMobs, targetModMobs, otherHostileMobs, tickingBlockEntities, 0, 0, 0, 0,
                0, 0, 0, 0, 0);
    }

    public ChunkPressureEntry(String dimension, long packedChunk, int trackedMobs, int targetModMobs,
                              int otherHostileMobs, int tickingBlockEntities, int sleepingMobs, int throttledMobs,
                              int lightingTasks) {
        this(dimension, packedChunk, trackedMobs, targetModMobs, otherHostileMobs, tickingBlockEntities, sleepingMobs,
                throttledMobs, lightingTasks, 0, 0, 0, 0, 0, 0);
    }

    public ChunkPressureEntry(String dimension, long packedChunk, int trackedMobs, int targetModMobs,
                              int otherHostileMobs, int tickingBlockEntities, int sleepingMobs, int throttledMobs,
                              int lightingTasks, int itemEntities) {
        this(dimension, packedChunk, trackedMobs, targetModMobs, otherHostileMobs, tickingBlockEntities, sleepingMobs,
                throttledMobs, lightingTasks, itemEntities, 0, 0, 0, 0, 0);
    }

    public ChunkPressureEntry(String dimension, long packedChunk, int trackedMobs, int targetModMobs,
                              int otherHostileMobs, int tickingBlockEntities, int sleepingMobs, int throttledMobs,
                              int lightingTasks, int itemEntities, int scheduledTicks, int nonMobEntities,
                              int pathfindingMobs) {
        this(dimension, packedChunk, trackedMobs, targetModMobs, otherHostileMobs, tickingBlockEntities, sleepingMobs,
                throttledMobs, lightingTasks, itemEntities, scheduledTicks, nonMobEntities, pathfindingMobs, 0, 0);
    }

    public long riskScore() {
        return (long) trackedMobs + (long) targetModMobs * 3L + (long) otherHostileMobs * 2L;
    }
}
