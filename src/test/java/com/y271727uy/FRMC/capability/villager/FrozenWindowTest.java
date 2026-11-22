package com.y271727uy.FRMC.capability.villager;

import com.y271727uy.FRMC.capability.entity.manager.village.FrozenWindow;
import org.junit.jupiter.api.Test;

import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.*;

class FrozenWindowTest {
    /** Vanilla adult villager work hours. */
    private static final IntPredicate WORK = dayTime -> dayTime >= 2000 && dayTime < 9000;

    @Test
    void freezeInsideWorkHoursOverlaps() {
        assertTrue(FrozenWindow.overlaps(3000, 100, WORK));
    }

    @Test
    void nightOnlyFreezeDoesNotOverlap() {
        assertFalse(FrozenWindow.overlaps(13000, 8000, WORK));
    }

    @Test
    void freezeSpanningMidnightIntoWorkOverlaps() {
        // 22000 -> 2500 the next day.
        assertTrue(FrozenWindow.overlaps(22000, 4500, WORK));
        assertFalse(FrozenWindow.overlaps(22000, 3900, WORK), "ends at 1900, before work starts");
    }

    @Test
    void absoluteDayTimeIsReducedModuloOneDay() {
        assertTrue(FrozenWindow.overlaps(5L * 24000L + 3000L, 50, WORK));
    }

    @Test
    void fullDayOrLongerAlwaysOverlaps() {
        assertTrue(FrozenWindow.overlaps(10000, 24000, WORK));
        assertTrue(FrozenWindow.overlaps(10000, 100000, WORK));
    }

    @Test
    void zeroDurationNeverOverlaps() {
        assertFalse(FrozenWindow.overlaps(3000, 0, WORK));
    }
}
