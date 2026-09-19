package com.y271727uy.FRMC.capability.blockchecking;

import com.y271727uy.FRMC.config.EntityActivityConfig;
import com.y271727uy.FRMC.capability.blockchecking.aggregate.ChunkPressureIndex;
import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkPressureIndexTest {
    @Test
    void mobOnlyUsesWeightAndStaysGreen() {
        assertEquals(64, EntityActivityConfig.overloadCleanupPressureThreshold());
        assertEquals(12L, ChunkPressureIndex.score(8, 0, 5, 0));
        assertEquals(ChunkPressureIndex.Band.GREEN, ChunkPressureIndex.band(12L));
        assertEquals(0xB030D060, ChunkPressureIndex.colorArgb(12L));
    }

    @Test
    void blockEntitiesOnlyUseTheirWeight() {
        assertEquals(10L, ChunkPressureIndex.score(0, 0, 0, 32));
        assertEquals(ChunkPressureIndex.Band.PALE_GREEN, ChunkPressureIndex.band(10L));
    }

    @Test
    void itemsOnlyUseTheirWeight() {
        assertEquals(7L, ChunkPressureIndex.score(0, 0, 0, 0, 32, 0));
        assertEquals(ChunkPressureIndex.Band.PALE_GREEN, ChunkPressureIndex.band(7L));
    }

    @Test
    void lightingOnlyUsesItsWeight() {
        assertEquals(3L, ChunkPressureIndex.score(0, 0, 0, 0, 0, 8));
        assertEquals(ChunkPressureIndex.Band.PALE_GREEN, ChunkPressureIndex.band(3L));
    }

    @Test
    void saturatedMobsReachWeightedScoreThenKeepClimbing() {
        assertEquals(60L, ChunkPressureIndex.score(64, 0, 0, 0));
        assertEquals(938L, ChunkPressureIndex.score(1000, 0, 0, 0));
        assertEquals(ChunkPressureIndex.Band.YELLOW, ChunkPressureIndex.band(60L));
        assertEquals(ChunkPressureIndex.Band.CRIMSON, ChunkPressureIndex.band(938L));
        assertEquals(60L, ChunkPressureIndex.score(0, 32, 0, 0));
        assertEquals(17L, ChunkPressureIndex.score(0, 0, 0, 32, 32, 0));
        assertEquals(ChunkPressureIndex.Band.GREEN, ChunkPressureIndex.band(17L));
    }

    @Test
    void scheduledTicksOnlyUseTheirWeight() {
        assertEquals(12L, ChunkPressureIndex.score(0, 0, 0, 0, 0, 0, 32));
        assertEquals(ChunkPressureIndex.Band.GREEN, ChunkPressureIndex.band(12L));
    }

    @Test
    void postLightsOnlyUseTheirWeight() {
        assertEquals(15L, ChunkPressureIndex.score(0, 0, 0, 0, 0, 0, 0, 16));
        assertEquals(ChunkPressureIndex.Band.GREEN, ChunkPressureIndex.band(15L));
    }

    @Test
    void postLightsAddOnTopOfMobs() {
        assertEquals(60L, ChunkPressureIndex.score(64, 0, 0, 0));
        assertEquals(75L, ChunkPressureIndex.score(64, 0, 0, 0, 0, 0, 0, 16));
        assertEquals(ChunkPressureIndex.Band.GOLD, ChunkPressureIndex.band(75L));
    }

    @Test
    void extraSignalsAddPastSaturatedMobs() {
        assertEquals(80L, ChunkPressureIndex.score(64, 0, 0, 32, 32, 8));
        assertEquals(92L, ChunkPressureIndex.score(64, 0, 0, 32, 32, 8, 32));
        assertEquals(ChunkPressureIndex.Band.GOLD, ChunkPressureIndex.band(80L));
        assertEquals(ChunkPressureIndex.Band.ORANGE, ChunkPressureIndex.band(92L));
        assertEquals(0xB0FF8818, ChunkPressureIndex.colorArgb(92L));
    }

    @Test
    void extrasAddOnTopOfMobs() {
        assertEquals(22L, ChunkPressureIndex.score(8, 0, 5, 32));
        assertEquals(ChunkPressureIndex.Band.GREEN, ChunkPressureIndex.band(22L));
        assertEquals(34L, ChunkPressureIndex.score(8, 0, 5, 32, 0, 0, 32));
        assertEquals(ChunkPressureIndex.Band.LIME, ChunkPressureIndex.band(34L));
    }

    @Test
    void compositeBandsUseSteppedCuts() {
        assertEquals(ChunkPressureIndex.Band.PALE_GREEN, ChunkPressureIndex.band(0L));
        assertEquals(ChunkPressureIndex.Band.PALE_GREEN, ChunkPressureIndex.band(10L));
        assertEquals(ChunkPressureIndex.Band.GREEN, ChunkPressureIndex.band(11L));
        assertEquals(ChunkPressureIndex.Band.GREEN, ChunkPressureIndex.band(20L));
        assertEquals(ChunkPressureIndex.Band.GREEN, ChunkPressureIndex.band(30L));
        assertEquals(ChunkPressureIndex.Band.LIME, ChunkPressureIndex.band(31L));
        assertEquals(ChunkPressureIndex.Band.LIME, ChunkPressureIndex.band(42L));
        assertEquals(ChunkPressureIndex.Band.LIME, ChunkPressureIndex.band(50L));
        assertEquals(ChunkPressureIndex.Band.YELLOW, ChunkPressureIndex.band(51L));
        assertEquals(ChunkPressureIndex.Band.YELLOW, ChunkPressureIndex.band(70L));
        assertEquals(ChunkPressureIndex.Band.GOLD, ChunkPressureIndex.band(71L));
        assertEquals(ChunkPressureIndex.Band.GOLD, ChunkPressureIndex.band(90L));
        assertEquals(ChunkPressureIndex.Band.ORANGE, ChunkPressureIndex.band(91L));
        assertEquals(ChunkPressureIndex.Band.ORANGE, ChunkPressureIndex.band(110L));
        assertEquals(ChunkPressureIndex.Band.RED, ChunkPressureIndex.band(111L));
        assertEquals(ChunkPressureIndex.Band.RED, ChunkPressureIndex.band(130L));
        assertEquals(ChunkPressureIndex.Band.CRIMSON, ChunkPressureIndex.band(131L));
        assertEquals(ChunkPressureIndex.Band.CRIMSON, ChunkPressureIndex.band(140L));
        assertEquals(ChunkPressureIndex.Band.CRIMSON, ChunkPressureIndex.band(141L));
        assertEquals(ChunkPressureIndex.Band.CRIMSON, ChunkPressureIndex.band(190L));
    }

    @Test
    void overlayColorsMatchBands() {
        assertEquals(0xB090E8B0, ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.PALE_GREEN));
        assertEquals(0xB030D060, ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.GREEN));
        assertEquals(0xB0C8E038, ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.LIME));
        assertEquals(0xB0FFE040, ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.YELLOW));
        assertEquals(0xB0FFB020, ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.GOLD));
        assertEquals(0xB0FF8818, ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.ORANGE));
        assertEquals(0xB0FF3838, ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.RED));
        assertEquals(0xB0A00818, ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.CRIMSON));
    }

    @Test
    void heatmapFillUsesBandColor() {
        assertEquals(ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.PALE_GREEN),
                ChunkPressureIndex.colorArgb(0L));
        assertEquals(ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.PALE_GREEN),
                ChunkPressureIndex.colorArgb(10L));
        assertEquals(ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.GREEN),
                ChunkPressureIndex.colorArgb(25L));
        assertEquals(ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.LIME),
                ChunkPressureIndex.colorArgb(49L));
        assertEquals(ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.LIME),
                ChunkPressureIndex.colorArgb(50L));
        assertEquals(ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.YELLOW),
                ChunkPressureIndex.colorArgb(51L));
        assertEquals(ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.YELLOW),
                ChunkPressureIndex.colorArgb(65L));
        assertEquals(ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.GOLD),
                ChunkPressureIndex.colorArgb(74L));
        assertEquals(ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.GOLD),
                ChunkPressureIndex.colorArgb(90L));
        assertEquals(ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.ORANGE),
                ChunkPressureIndex.colorArgb(100L));
        assertEquals(ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.RED),
                ChunkPressureIndex.colorArgb(120L));
        assertEquals(ChunkPressureIndex.colorArgb(ChunkPressureIndex.Band.CRIMSON),
                ChunkPressureIndex.colorArgb(132L));
        assertEquals(ChunkPressureIndex.colorArgb(0L), ChunkPressureIndex.colorArgb(-4L));
        assertEquals(ChunkPressureIndex.colorArgb(140L), ChunkPressureIndex.colorArgb(141L));
        assertEquals(ChunkPressureIndex.colorArgb(140L), ChunkPressureIndex.colorArgb(190L));
        assertEquals(ChunkPressureIndex.colorArgb(140L), ChunkPressureIndex.colorArgb(200L));
    }

    @Test
    void negativeCountsClampToZero() {
        assertEquals(0L, ChunkPressureIndex.score(-3, -1, -9, -4, -2, -8));
        assertEquals(ChunkPressureIndex.Band.PALE_GREEN, ChunkPressureIndex.band(-1L));
    }

    @Test
    void entryScoreReadsBlockEntitiesAndLeavesCommandLineRiskAlone() {
        ChunkPressureEntry withoutBe = new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5);
        assertEquals(18L, withoutBe.riskScore());
        assertEquals(0, withoutBe.tickingBlockEntities());
        assertEquals(12L, ChunkPressureIndex.score(withoutBe));

        ChunkPressureEntry withBe = new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32);
        assertEquals(18L, withBe.riskScore());
        assertEquals(22L, ChunkPressureIndex.score(withBe));
    }

    @Test
    void sleepingAndThrottledDoNotChangeIndexOrCommandLineRisk() {
        ChunkPressureEntry extra = new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32, 7, 3, 0, 0);
        assertEquals(7, extra.sleepingMobs());
        assertEquals(3, extra.throttledMobs());
        assertEquals(0, extra.pendingThrottleMobs());
        assertEquals(0, extra.pendingSleepMobs());
        assertEquals(18L, extra.riskScore());
        assertEquals(22L, ChunkPressureIndex.score(extra));
        assertEquals(0, new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32).sleepingMobs());
    }

    @Test
    void lightingAndItemsChangeIndexButNotCommandLineRisk() {
        ChunkPressureEntry extra = new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32, 0, 0, 8, 32);
        assertEquals(8, extra.lightingTasks());
        assertEquals(32, extra.itemEntities());
        assertEquals(18L, extra.riskScore());
        assertEquals(32L, ChunkPressureIndex.score(extra));
        assertEquals(0, new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32).itemEntities());
        assertEquals(0, new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32, 0, 0, 12).itemEntities());
    }

    @Test
    void scheduledTicksChangeIndexButNotCommandLineRisk() {
        ChunkPressureEntry extra = new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32, 0, 0, 0, 0, 32, 0, 0);
        assertEquals(32, extra.scheduledTicks());
        assertEquals(18L, extra.riskScore());
        assertEquals(34L, ChunkPressureIndex.score(extra));
        assertEquals(0, new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32).scheduledTicks());
        assertEquals(0, new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32, 0, 0, 12, 8).scheduledTicks());
    }

    @Test
    void nonMobAndPathfindingDoNotChangeIndexOrCommandLineRisk() {
        ChunkPressureEntry extra = new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32, 0, 0, 0, 0, 0, 9, 4);
        assertEquals(9, extra.nonMobEntities());
        assertEquals(4, extra.pathfindingMobs());
        assertEquals(18L, extra.riskScore());
        assertEquals(22L, ChunkPressureIndex.score(extra));
        assertEquals(0, new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32).nonMobEntities());
        assertEquals(0, new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32).pathfindingMobs());
    }

    @Test
    void freezeScoreUsesTrackedMobsOnlyAndDoesNotEnterComposite() {
        assertEquals(13L, ChunkPressureIndex.freezeScore(8));
        assertEquals(31L, ChunkPressureIndex.freezeScore(20));
        assertEquals(100L, ChunkPressureIndex.freezeScore(64));
        assertEquals(1563L, ChunkPressureIndex.freezeScore(1000));
        assertEquals(0L, ChunkPressureIndex.freezeScore(-4));

        ChunkPressureEntry extra = new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32, 7, 3, 8, 32, 32, 9, 4);
        assertEquals(13L, ChunkPressureIndex.freezeScore(extra));
        assertEquals(12L, ChunkPressureIndex.score(8, 0, 5, 0));
        assertEquals(44L, ChunkPressureIndex.score(extra));
        assertEquals(18L, extra.riskScore());
    }

    @Test
    void pendingFreezeAndThrottleDoNotChangeIndex() {
        ChunkPressureEntry extra = new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32, 7, 3, 0, 0, 0, 0, 0,
                4, 6);
        assertEquals(4, extra.pendingThrottleMobs());
        assertEquals(6, extra.pendingSleepMobs());
        assertEquals(18L, extra.riskScore());
        assertEquals(22L, ChunkPressureIndex.score(extra));
        assertEquals(13L, ChunkPressureIndex.freezeScore(extra));
        assertEquals(0, new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32).pendingThrottleMobs());
        assertEquals(0, new ChunkPressureEntry("minecraft:overworld", 1L, 8, 0, 5, 32).pendingSleepMobs());
    }
}
