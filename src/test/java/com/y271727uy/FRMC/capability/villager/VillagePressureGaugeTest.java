package com.y271727uy.FRMC.capability.villager;

import com.y271727uy.FRMC.capability.entity.manager.village.VillagePressureGauge;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VillagePressureGaugeTest {
    @Test
    void engagesAtThresholdAndStaysBetweenThresholds() {
        VillagePressureGauge gauge = new VillagePressureGauge(45.0F, 35.0F, 0);
        assertFalse(gauge.update(44.9F, 0));
        assertTrue(gauge.update(45.0F, 1));
        assertTrue(gauge.update(40.0F, 2), "between release and engage keeps pressure on");
        assertFalse(gauge.update(35.0F, 3));
        assertFalse(gauge.update(40.0F, 4), "between thresholds does not re-engage");
    }

    @Test
    void holdsForMinimumTimeEvenWhenLoadDrops() {
        VillagePressureGauge gauge = new VillagePressureGauge(45.0F, 35.0F, 200);
        assertTrue(gauge.update(50.0F, 100));
        assertTrue(gauge.update(10.0F, 299));
        assertFalse(gauge.update(10.0F, 300));
    }

    @Test
    void releaseNeverExceedsEngage() {
        VillagePressureGauge gauge = new VillagePressureGauge(30.0F, 60.0F, 0);
        assertEquals(30.0F, gauge.releaseMspt());
    }
}
