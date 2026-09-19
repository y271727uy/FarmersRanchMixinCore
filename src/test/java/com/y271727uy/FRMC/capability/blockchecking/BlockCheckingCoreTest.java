package com.y271727uy.FRMC.capability.blockchecking;

import com.y271727uy.FRMC.capability.blockchecking.aggregate.ChunkPressureOrderer;
import com.y271727uy.FRMC.capability.blockchecking.aggregate.BlockCheckingLimits;
import com.y271727uy.FRMC.capability.blockchecking.aggregate.ReportAggregator;
import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureEntry;
import com.y271727uy.FRMC.capability.blockchecking.model.BlockCheckingReport;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkKind;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkObservation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockCheckingCoreTest {
    @Test
    void aggregatesAndLimitsSamples() {
        ReportAggregator aggregator = new ReportAggregator();
        for (int i = 0; i < BlockCheckingLimits.MAX_SAMPLES + 3; i++) {
            aggregator.record(new WorkObservation(i, WorkKind.ENTITY_AI, 10, 1, i));
        }
        BlockCheckingReport report = aggregator.finish(0, 100, 1);
        assertEquals(BlockCheckingLimits.MAX_SAMPLES, report.retainedSamples());
        assertEquals(3, report.discardedSamples());
        assertEquals(1, report.metrics().size());
        assertEquals(BlockCheckingLimits.MAX_SAMPLES, report.metrics().get(0).samples());
    }

    @Test
    void chunkOrderingUsesRiskThenStableTieBreak() {
        List<ChunkPressureEntry> result = ChunkPressureOrderer.sortAndLimit(List.of(
                new ChunkPressureEntry("minecraft:overworld", 4L, 2, 0, 0),
                new ChunkPressureEntry("minecraft:overworld", 2L, 1, 1, 0),
                new ChunkPressureEntry("minecraft:nether", 1L, 1, 1, 0)), 2);
        assertEquals(2, result.size());
        assertEquals("minecraft:nether", result.get(0).dimension());
        assertEquals(1L, result.get(0).packedChunk());
        assertEquals("minecraft:overworld", result.get(1).dimension());
        assertEquals(2L, result.get(1).packedChunk());
    }
}
