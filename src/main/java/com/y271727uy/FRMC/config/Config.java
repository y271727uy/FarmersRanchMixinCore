package com.y271727uy.FRMC.config;

/**
 * Mod configuration for FRMC.
 */
public final class Config {
    /** Master switch for recipe search optimization */
    public static boolean recipeSearchEnabled = true;

    /** Matches Fast-Recipe-Search's compatibility-safe default scope. */
    public static boolean recipeSearchOptimizeOnlyVanilla = true;

    /** Compact Ingredient network encoding; disabled by default for compatibility. */
    public static boolean recipeIngredientSyncEnabled = false;

    /** Ingredient instance deduplication; disabled by default for compatibility. */
    public static boolean recipeIngredientDeduplicatorEnabled = false;

    /** Enables low-noise OpenGL diagnostic logging for frame stalls and context info. */
    public static boolean openglDiagnosticsEnabled = false;

    /** Enables 1% low frame-time statistics derived from Minecraft's frame timer. */
    public static boolean openglLowPercentStatsEnabled = false;

    /** Enables warnings when RenderSystem is called from the wrong thread. */
    public static boolean openglThreadViolationDetectionEnabled = false;

    /** Enables GPU memory availability logging when the driver exposes a supported OpenGL extension. */
    public static boolean openglGpuMemoryMonitoringEnabled = false;

    /** Enables per-frame render phase timing logs. */
    public static boolean openglFramePhaseBreakdownEnabled = false;

    /** Frame gap threshold in milliseconds before logging a stall warning. */
    public static long openglStutterThresholdMs = 75L;

    /** Minimum gap in milliseconds between 1% low summary logs. */
    public static long openglLowPercentLogIntervalMs = 1000L;

    /** Minimum gap in milliseconds between GPU memory summary logs. */
    public static long openglGpuMemoryLogIntervalMs = 5000L;

    /** Minimum gap in milliseconds between per-phase render breakdown logs. */
    public static long openglFramePhaseLogIntervalMs = 1000L;

    /** Minimum gap in milliseconds between render-thread violation logs. */
    public static long openglThreadViolationLogIntervalMs = 1000L;


    private Config() {
    }
}
