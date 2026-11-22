package com.y271727uy.FRMC.capability.entity.manager.village;

/**
 * Minecraft-free server load switch with hysteresis. Pressure turns on at the engage MSPT and only turns off
 * once MSPT has dropped to the release value and the minimum hold time has passed, so freezing villagers
 * (which lowers MSPT) does not make the switch flap.
 */
public final class VillagePressureGauge {
    private final float engageMspt;
    private final float releaseMspt;
    private final int holdTicks;
    private boolean high;
    private long engagedAtTick;

    public VillagePressureGauge(float engageMspt, float releaseMspt, int holdTicks) {
        this.engageMspt = engageMspt;
        this.releaseMspt = Math.min(releaseMspt, engageMspt);
        this.holdTicks = Math.max(0, holdTicks);
    }

    public boolean update(float mspt, long tick) {
        if (!high) {
            if (mspt >= engageMspt) {
                high = true;
                engagedAtTick = tick;
            }
        } else if (mspt <= releaseMspt && tick - engagedAtTick >= holdTicks) {
            high = false;
        }
        return high;
    }

    public boolean high() {
        return high;
    }

    public float engageMspt() {
        return engageMspt;
    }

    public float releaseMspt() {
        return releaseMspt;
    }
}
