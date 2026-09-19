package com.y271727uy.FRMC.capability.blockchecking.aggregate;

/** Hard resource limits independent from editable configuration. */
public final class BlockCheckingLimits {
    public static final int MAX_CONTEXT_DEPTH = 16;
    public static final int MAX_SAMPLES = 4_096;
    public static final int MAX_OUTPUT_ENTRIES = 128;
    public static final int MIN_SESSION_SECONDS = 1;
    public static final int MAX_SESSION_SECONDS = 3_600;
    public static final int MIN_SAMPLE_INTERVAL_MILLIS = 1;
    public static final int MAX_SAMPLE_INTERVAL_MILLIS = 10_000;
    public static final int MIN_CHUNK_SNAPSHOT_PERIOD_TICKS = 1;
    public static final int MAX_CHUNK_SNAPSHOT_PERIOD_TICKS = 1_200;

    private BlockCheckingLimits() {
    }

    public static int bounded(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
