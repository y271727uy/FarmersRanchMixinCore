package com.y271727uy.FRMC.capability.betterfoliagecutting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BetterFoliageInteriorCullingTest {
    @Test
    void sixLeafNeighborsAreInterior() {
        int mask = BetterFoliageInteriorCulling.collectMask(direction3d -> true);
        assertEquals(BetterFoliageInteriorCulling.INTERIOR_MASK, mask);
        assertTrue(BetterFoliageInteriorCulling.shouldCullEntirely(mask));
        assertTrue(BetterFoliageInteriorCulling.shouldDropCrosses(mask));
    }

    @Test
    void missingOneFaceIsNotInterior() {
        int mask = BetterFoliageInteriorCulling.collectMask(
            direction3d -> direction3d != BetterFoliageInteriorCulling.UP
        );
        assertFalse(BetterFoliageInteriorCulling.shouldCullEntirely(mask));
        assertTrue(BetterFoliageInteriorCulling.shouldDropCrosses(mask));
    }

    @Test
    void nonLeafNeighborsStayZero() {
        assertEquals(0, BetterFoliageInteriorCulling.collectMask(direction3d -> false));
        assertFalse(BetterFoliageInteriorCulling.shouldCullEntirely(0));
        assertFalse(BetterFoliageInteriorCulling.shouldDropCrosses(0));
    }

    @Test
    void northLeafNeighborDropsCrossesOnEverySide() {
        int mask = BetterFoliageInteriorCulling.collectMask(
            direction3d -> direction3d == BetterFoliageInteriorCulling.NORTH
        );
        assertTrue(BetterFoliageInteriorCulling.shouldDropCrosses(mask));
        assertFalse(BetterFoliageInteriorCulling.shouldCullEntirely(mask));
    }

    @Test
    void southLeafNeighborDropsCrossesOnEverySide() {
        int mask = BetterFoliageInteriorCulling.collectMask(
            direction3d -> direction3d == BetterFoliageInteriorCulling.SOUTH
        );
        assertTrue(BetterFoliageInteriorCulling.shouldDropCrosses(mask));
        assertFalse(BetterFoliageInteriorCulling.shouldCullEntirely(mask));
    }

    @Test
    void southAirWithNorthLeafStillDropsCrosses() {
        int mask = BetterFoliageInteriorCulling.collectMask(
            direction3d -> direction3d != BetterFoliageInteriorCulling.SOUTH
        );
        assertTrue(BetterFoliageInteriorCulling.shouldDropCrosses(mask));
        assertFalse(BetterFoliageInteriorCulling.shouldCullEntirely(mask));
    }

    @Test
    void onlyEastWestLeavesKeepCrosses() {
        int mask = BetterFoliageInteriorCulling.collectMask(
            direction3d -> direction3d == BetterFoliageInteriorCulling.EAST
                || direction3d == BetterFoliageInteriorCulling.WEST
        );
        assertFalse(BetterFoliageInteriorCulling.shouldDropCrosses(mask));
        assertFalse(BetterFoliageInteriorCulling.shouldCullEntirely(mask));
    }

    @Test
    void northAndSouthExposedKeepsCrosses() {
        int mask = BetterFoliageInteriorCulling.collectMask(
            direction3d -> direction3d != BetterFoliageInteriorCulling.NORTH
                && direction3d != BetterFoliageInteriorCulling.SOUTH
        );
        assertFalse(BetterFoliageInteriorCulling.shouldDropCrosses(mask));
        assertFalse(BetterFoliageInteriorCulling.shouldCullEntirely(mask));
    }
}
