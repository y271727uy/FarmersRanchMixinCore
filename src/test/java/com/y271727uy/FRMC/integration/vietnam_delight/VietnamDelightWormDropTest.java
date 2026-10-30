package com.y271727uy.FRMC.integration.vietnam_delight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class VietnamDelightWormDropTest {
    @Test
    void dropsASingleWormOnlyOnZero() {
        assertEquals(1, VietnamDelightWormDrop.roll(bound -> 0, VietnamDelightWormDrop.HOE_TILL_ONE_IN));
        assertEquals(0, VietnamDelightWormDrop.roll(bound -> 1, VietnamDelightWormDrop.HOE_TILL_ONE_IN));
        assertEquals(1, VietnamDelightWormDrop.roll(bound -> 0, VietnamDelightWormDrop.BREAK_ONE_IN));
        assertEquals(0, VietnamDelightWormDrop.roll(bound -> 47, VietnamDelightWormDrop.BREAK_ONE_IN));
    }

    @Test
    void refusesANonPositiveDenominator() {
        assertEquals(0, VietnamDelightWormDrop.roll(bound -> 0, 0));
    }

    @Test
    void stopsAtTheBlockUnderfoot() {
        AtomicInteger samples = new AtomicInteger();
        boolean near = VietnamDelightWormDrop.nearRiver(0, 0, (x, z) -> {
            samples.incrementAndGet();
            return x == 0 && z == 0 ? "river" : "plains";
        });
        assertTrue(near);
        assertEquals(1, samples.get());
    }

    @Test
    void skipsUnloadedColumnsAndStillFindsALoadedRiver() {
        AtomicInteger samples = new AtomicInteger();
        boolean near = VietnamDelightWormDrop.nearRiver(0, 0, (x, z) -> {
            samples.incrementAndGet();
            if (x == 16 && z == 16) {
                return "frozen_river";
            }
            return null;
        });
        assertTrue(near);
        assertTrue(samples.get() > 1);
        assertTrue(samples.get() <= 81);
    }

    @Test
    void doesNotTreatADryBiomeAsARiver() {
        assertFalse(VietnamDelightWormDrop.nearRiver(0, 0, (x, z) -> "birch_forest"));
        assertFalse(VietnamDelightWormDrop.containsRiver(null));
        assertTrue(VietnamDelightWormDrop.containsRiver("river"));
    }
}
