package com.y271727uy.FRMC;

/**
 * Mod configuration for FRMC.
 */
public final class Config {
    /** Master switch for recipe search optimization */
    public static boolean recipeSearchEnabled = true;

    /** Enables low-noise OpenGL diagnostic logging for frame stalls and context info. */
    public static boolean openglDiagnosticsEnabled = true;

    /** Frame gap threshold in milliseconds before logging a stall warning. */
    public static long openglStutterThresholdMs = 75L;

    private Config() {
    }
}
