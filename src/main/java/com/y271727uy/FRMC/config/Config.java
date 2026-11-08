package com.y271727uy.FRMC.config;

/**
 * Mod configuration for FRMC.
 */
public final class Config {
    /** Enables low-overhead aggregated UW/Citadel/AR render diagnostics. */
    public static boolean entityRenderDiagnosticsEnabled = false;

    /** Enables combined ZYX rotations and per-part quaternion reuse for Citadel models. */
    public static volatile boolean citadelCombinedRotationEnabled = true;

    /** Enables the experimental recursive Citadel traversal on the AR path. */
    public static boolean citadelFastTraversalEnabled = false;

    /** Enables the diagnostic-only Citadel SoA transform shadow evaluator. */
    public static volatile boolean citadelSoaShadowEnabled = false;

    /** Enables diagnostic-only multi-entity batch snapshot and reference validation. */
    public static volatile boolean citadelBatchValidationEnabled = false;

    /** Runs a one-shot GPU pose comparison when entity render diagnostics are enabled. */
    public static boolean citadelGpuPoseValidationEnabled = false;

    /** Uses the experimental Citadel GPU pose path. */
    public static volatile boolean citadelGpuPoseRenderingEnabled = false;

    /** Extends GPU pose rendering to other compatible Citadel models with a merged material mesh. */
    public static volatile boolean citadelGpuGeneralModelsEnabled = false;

    /** Submits one mesh per material on the supported GPU pose path. */
    public static volatile boolean citadelGpuModelBatchingEnabled = true;

    /** Measures boar underside visibility once on the GPU material path. */
    public static boolean citadelBottomFaceProbeEnabled = false;

    /** Skips rasterization of boar undersides fully covered by terrain depth. */
    public static volatile boolean citadelBottomFaceCullingEnabled = false;

    /** Tests each supported Citadel part against the current frustum and terrain depth. */
    public static volatile boolean citadelPartCullingEnabled = false;

    /** Selects reduced meshes for small on-screen boars on the AR material path. */
    public static volatile boolean citadelBoarLodEnabled = false;

    /** Omits small projected details on compatible Citadel material batches. */
    public static volatile boolean citadelGeneralLodEnabled = false;

    public static boolean recipeSearchEnabled = true;

    /** Restricts indexed searches to vanilla RecipeTypes; disabled by default to match the generic search scope. */
    public static boolean recipeSearchOptimizeOnlyVanilla = false;

    /** Restricts indexed recipes to vanilla implementation classes; disabled by default. */
    public static boolean recipeSearchOptimizeOnlyVanillaRecipeClasses = false;

    /** Compact Ingredient network encoding; disabled by default for compatibility. */
    public static boolean recipeIngredientSyncEnabled = false;

    /** Ingredient instance deduplication; disabled by default for compatibility. */
    public static boolean recipeIngredientDeduplicatorEnabled = false;

    /** Enables low-noise OpenGL frame-probe logging for frame stalls and context info. */
    public static boolean openglFrameProbeEnabled = false;

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
