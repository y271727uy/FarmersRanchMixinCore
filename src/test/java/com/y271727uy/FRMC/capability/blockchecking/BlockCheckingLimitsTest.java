package com.y271727uy.FRMC.capability.blockchecking;

import com.y271727uy.FRMC.capability.blockchecking.aggregate.BlockCheckingLimits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockCheckingLimitsTest {
    @Test
    void hardBoundsClampBothDirections() {
        assertEquals(1, BlockCheckingLimits.bounded(-4, 1, 20));
        assertEquals(20, BlockCheckingLimits.bounded(99, 1, 20));
        assertEquals(7, BlockCheckingLimits.bounded(7, 1, 20));
    }
}
