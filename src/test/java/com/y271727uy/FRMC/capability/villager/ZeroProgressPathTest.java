package com.y271727uy.FRMC.capability.villager;

import com.y271727uy.FRMC.capability.villager.optimization.ZeroProgressPath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZeroProgressPathTest {
    @Test
    void partialPathEndingUnderfootIsZeroProgress() {
        // The case from the profile: villager at 221,-58,67, target above, path end 221,-58,67 with one node.
        assertTrue(ZeroProgressPath.isZeroProgress(false, 221, -58, 67, 221, -58, 67));
    }

    @Test
    void toleratesOneBlockStartHeightDifference() {
        assertTrue(ZeroProgressPath.isZeroProgress(false, 5, 64, 5, 5, 63, 5));
        assertTrue(ZeroProgressPath.isZeroProgress(false, 5, 63, 5, 5, 64, 5));
        assertFalse(ZeroProgressPath.isZeroProgress(false, 5, 66, 5, 5, 64, 5));
    }

    @Test
    void reachablePathsAreNeverRejected() {
        assertFalse(ZeroProgressPath.isZeroProgress(true, 221, -58, 67, 221, -58, 67));
    }

    @Test
    void partialPathsThatMoveTheVillagerAreKept() {
        // A partial path that gets closer is useful vanilla behavior (the profile's 45-node partial paths).
        assertFalse(ZeroProgressPath.isZeroProgress(false, 227, -60, 44, 216, -59, 87));
        assertFalse(ZeroProgressPath.isZeroProgress(false, 222, -58, 67, 221, -58, 67));
    }
}
