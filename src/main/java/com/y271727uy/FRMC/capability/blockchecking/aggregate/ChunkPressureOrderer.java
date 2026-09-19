package com.y271727uy.FRMC.capability.blockchecking.aggregate;

import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureEntry;

import java.util.Comparator;
import java.util.List;

/** Sorts primitive tracker copies without requiring a game world or entity instance. */
public final class ChunkPressureOrderer {
    private static final Comparator<ChunkPressureEntry> RISK_ORDER = Comparator
            .comparingLong(ChunkPressureEntry::riskScore).reversed()
            .thenComparing(Comparator.comparingInt(ChunkPressureEntry::trackedMobs).reversed())
            .thenComparing(ChunkPressureEntry::dimension)
            .thenComparingLong(ChunkPressureEntry::packedChunk);

    private ChunkPressureOrderer() {
    }

    public static List<ChunkPressureEntry> sortAndLimit(List<ChunkPressureEntry> entries, int limit) {
        int boundedLimit = BlockCheckingLimits.bounded(limit, 0, BlockCheckingLimits.MAX_OUTPUT_ENTRIES);
        return entries.stream().sorted(RISK_ORDER).limit(boundedLimit).toList();
    }
}
