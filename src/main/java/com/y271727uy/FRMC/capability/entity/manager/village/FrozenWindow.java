package com.y271727uy.FRMC.capability.entity.manager.village;

import java.util.function.IntPredicate;

/** Minecraft-free check of whether a frozen period covered part of a schedule window, such as work hours. */
public final class FrozenWindow {
    public static final int DAY_TICKS = 24000;
    /** Vanilla schedules change on whole hundreds of ticks, so sampling every 100 ticks cannot miss a window. */
    public static final int SAMPLE_STEP = 100;

    private FrozenWindow() {
    }

    /**
     * @param startDayTime day time (any value, taken modulo one day) when the freeze began
     * @param duration     frozen game ticks
     * @param inWindow     whether the schedule is in the window at a day time in [0, 24000)
     */
    public static boolean overlaps(long startDayTime, long duration, IntPredicate inWindow) {
        if (duration <= 0) {
            return false;
        }
        long span = Math.min(duration, DAY_TICKS);
        int start = (int) Math.floorMod(startDayTime, (long) DAY_TICKS);
        for (long offset = 0; offset < span; offset += SAMPLE_STEP) {
            if (inWindow.test((int) ((start + offset) % DAY_TICKS))) {
                return true;
            }
        }
        return inWindow.test((int) ((start + span) % DAY_TICKS));
    }
}
