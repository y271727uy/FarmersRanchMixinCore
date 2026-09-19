package com.y271727uy.FRMC.capability.blockchecking.aggregate;

import com.y271727uy.FRMC.config.EntityActivityConfig;
import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureEntry;

/**
 * Overlay composite index. Each signal has its own cost; weights do not need to sum to 1.
 * Reference saturations scale the number: 64 mobs reach 60 at the current mob weight, extras
 * add on top, and the score keeps climbing past 140 so the tooltip can show 141 or 190.
 * Color stops at crimson after 130. Freeze pressure is a separate display-only index that
 * reaches 100 at the same 64-mob reference. Non-mob entities, pathfinding, pending freeze
 * and pending throttle stay display-only. Embeddium rebuild/geometry stays display-only.
 * Lumenized point lights enter the composite on the overlay path via score(entry, postLights);
 * the server-side score(entry) path stays 0 for that slot.
 * Heatmap fill uses a stepped band color; there is no intra-band lerp.
 * Idle stays pale green through 10; later bands are 20 points wide through red.
 */
public final class ChunkPressureIndex {
    public enum Band {
        PALE_GREEN,
        GREEN,
        LIME,
        YELLOW,
        GOLD,
        ORANGE,
        RED,
        CRIMSON
    }

    public static final double MOB_WEIGHT = 0.60D;
    public static final double BLOCK_ENTITY_WEIGHT = 0.10D;
    public static final double ITEM_WEIGHT = 0.07D;
    public static final double LIGHT_WEIGHT = 0.03D;
    public static final double SCHEDULED_TICK_WEIGHT = 0.12D;
    public static final double POST_LIGHT_WEIGHT = 0.15D;
    public static final int BLOCK_ENTITY_SATURATION = 32;
    public static final int ITEM_SATURATION = 32;
    public static final int LIGHT_SATURATION = 8;
    public static final int SCHEDULED_TICK_SATURATION = 32;
    public static final int POST_LIGHT_SATURATION = 16;
    public static final int MAX_SCORE = 140;
    public static final int PALE_GREEN_MAX = 10;
    public static final int GREEN_MAX = 30;
    public static final int LIME_MAX = 50;
    public static final int YELLOW_MAX = 70;
    public static final int GOLD_MAX = 90;
    public static final int ORANGE_MAX = 110;
    public static final int RED_MAX = 130;

    private static final int[] BAND_RGB = {
            0x90E8B0,
            0x30D060,
            0xC8E038,
            0xFFE040,
            0xFFB020,
            0xFF8818,
            0xFF3838,
            0xA00818
    };

    private ChunkPressureIndex() {
    }

    public static long score(int trackedMobs, int targetModMobs, int otherHostileMobs, int tickingBlockEntities) {
        return score(trackedMobs, targetModMobs, otherHostileMobs, tickingBlockEntities, 0, 0);
    }

    public static long score(int trackedMobs, int targetModMobs, int otherHostileMobs, int tickingBlockEntities,
                             int itemEntities, int lightingTasks) {
        return score(trackedMobs, targetModMobs, otherHostileMobs, tickingBlockEntities, itemEntities, lightingTasks, 0);
    }

    public static long score(int trackedMobs, int targetModMobs, int otherHostileMobs, int tickingBlockEntities,
                             int itemEntities, int lightingTasks, int scheduledTicks) {
        return score(trackedMobs, targetModMobs, otherHostileMobs, tickingBlockEntities, itemEntities, lightingTasks,
                scheduledTicks, 0);
    }

    public static long score(int trackedMobs, int targetModMobs, int otherHostileMobs, int tickingBlockEntities,
                             int itemEntities, int lightingTasks, int scheduledTicks, int postLights) {
        double mobNorm = scale(mobRaw(trackedMobs, targetModMobs, otherHostileMobs)
                / (double) Math.max(1, EntityActivityConfig.overloadCleanupPressureThreshold()));
        double beNorm = scale(tickingBlockEntities / (double) BLOCK_ENTITY_SATURATION);
        double itemNorm = scale(itemEntities / (double) ITEM_SATURATION);
        double lightNorm = scale(lightingTasks / (double) LIGHT_SATURATION);
        double tickNorm = scale(scheduledTicks / (double) SCHEDULED_TICK_SATURATION);
        double postLightNorm = scale(postLights / (double) POST_LIGHT_SATURATION);
        long raw = Math.round(100.0D * (mobNorm * MOB_WEIGHT + beNorm * BLOCK_ENTITY_WEIGHT
                + itemNorm * ITEM_WEIGHT + lightNorm * LIGHT_WEIGHT + tickNorm * SCHEDULED_TICK_WEIGHT
                + postLightNorm * POST_LIGHT_WEIGHT));
        return Math.max(0L, raw);
    }

    public static long score(ChunkPressureEntry entry) {
        return score(entry, 0);
    }

    public static long score(ChunkPressureEntry entry, int postLights) {
        return score(entry.trackedMobs(), entry.targetModMobs(), entry.otherHostileMobs(),
                entry.tickingBlockEntities(), entry.itemEntities(), entry.lightingTasks(),
                entry.scheduledTicks(), postLights);
    }

    /**
     * Display-only index used by entity freeze/cleanup. 64 tracked mobs reach 100;
     * extras keep climbing. Sleeping and throttled counts do not feed this number.
     */
    public static long freezeScore(int trackedMobs) {
        double freezeNorm = scale(Math.max(0, trackedMobs)
                / (double) Math.max(1, EntityActivityConfig.overloadCleanupPressureThreshold()));
        return Math.max(0L, Math.round(100.0D * freezeNorm));
    }

    public static long freezeScore(ChunkPressureEntry entry) {
        return freezeScore(entry.trackedMobs());
    }

    public static Band band(long score) {
        long clamped = Math.max(0L, score);
        if (clamped <= PALE_GREEN_MAX) {
            return Band.PALE_GREEN;
        }
        if (clamped <= GREEN_MAX) {
            return Band.GREEN;
        }
        if (clamped <= LIME_MAX) {
            return Band.LIME;
        }
        if (clamped <= YELLOW_MAX) {
            return Band.YELLOW;
        }
        if (clamped <= GOLD_MAX) {
            return Band.GOLD;
        }
        if (clamped <= ORANGE_MAX) {
            return Band.ORANGE;
        }
        if (clamped <= RED_MAX) {
            return Band.RED;
        }
        return Band.CRIMSON;
    }

    public static int colorArgb(Band band) {
        return 0xB0000000 | BAND_RGB[band.ordinal()];
    }

    public static int colorArgb(long score) {
        return colorArgb(band(score));
    }

    static long mobRaw(int trackedMobs, int targetModMobs, int otherHostileMobs) {
        return (long) Math.max(0, trackedMobs)
                + (long) Math.max(0, targetModMobs) * 2L
                + (long) Math.max(0, otherHostileMobs);
    }

    private static double scale(double value) {
        return value <= 0.0D ? 0.0D : value;
    }
}
