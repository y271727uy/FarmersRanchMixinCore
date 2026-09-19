package com.y271727uy.FRMC.capability.blockchecking.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Bounded, read-only view of chunks represented by the entity activity tracker. */
public record ChunkPressureSnapshot(long capturedAtMillis, List<ChunkPressureEntry> entries, boolean trackedMobCoverage) {
    public ChunkPressureSnapshot {
        entries = List.copyOf(entries);
    }

    /**
     * Puts {@code entry} first. Same dimension+packed row is replaced; other rows stay in order.
     * Empty or null {@code base} becomes a one-row snapshot.
     */
    public static ChunkPressureSnapshot merge(ChunkPressureSnapshot base, ChunkPressureEntry entry, long capturedAtMillis) {
        return merge(base, entry, capturedAtMillis, Integer.MAX_VALUE);
    }

    public static ChunkPressureSnapshot merge(ChunkPressureSnapshot base, ChunkPressureEntry entry, long capturedAtMillis,
                                              int limit) {
        Objects.requireNonNull(entry, "entry");
        int bounded = Math.max(1, limit);
        List<ChunkPressureEntry> merged = new ArrayList<>();
        merged.add(entry);
        if (base != null) {
            for (ChunkPressureEntry existing : base.entries()) {
                if (merged.size() >= bounded) {
                    break;
                }
                if (existing.dimension().equals(entry.dimension()) && existing.packedChunk() == entry.packedChunk()) {
                    continue;
                }
                merged.add(existing);
            }
        }
        boolean coverage = base == null || base.trackedMobCoverage();
        return new ChunkPressureSnapshot(capturedAtMillis, merged, coverage);
    }

    /**
     * Drops the matching dimension+packed row. Unknown or empty {@code base} stays empty.
     * Coverage is unchanged.
     */
    public static ChunkPressureSnapshot without(ChunkPressureSnapshot base, String dimension, long packedChunk,
                                                long capturedAtMillis) {
        if (base == null) {
            return new ChunkPressureSnapshot(capturedAtMillis, List.of(), true);
        }
        if (dimension == null || dimension.isEmpty()) {
            return new ChunkPressureSnapshot(capturedAtMillis, base.entries(), base.trackedMobCoverage());
        }
        List<ChunkPressureEntry> kept = new ArrayList<>();
        for (ChunkPressureEntry existing : base.entries()) {
            if (existing.dimension().equals(dimension) && existing.packedChunk() == packedChunk) {
                continue;
            }
            kept.add(existing);
        }
        return new ChunkPressureSnapshot(capturedAtMillis, kept, base.trackedMobCoverage());
    }
}
