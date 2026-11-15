package com.y271727uy.FRMC.integration.xaero;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** Client-only settings; no Xaero or AR classes are needed to register them. */
public final class XaeroPerformanceConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue LAZY_HEIGHTS;
    private static final ForgeConfigSpec.BooleanValue SKIP_UPLOAD_BENCHMARK;
    private static final ForgeConfigSpec.BooleanValue DEDUPLICATE_LAYER_MARKS;
    private static final ForgeConfigSpec.BooleanValue ORPHAN_UPLOAD_PBO;
    private static final ForgeConfigSpec.BooleanValue BATCH_OVERLAY;
    private static final ForgeConfigSpec.IntValue VRAM_POLL_INTERVAL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        LAZY_HEIGHTS = builder.comment("Allocate a map tile's height arrays on first write, not construction.",
                "Existing lazy arrays remain safe if this setting is disabled later.")
                .define("lazy_height_arrays", true);
        SKIP_UPLOAD_BENCHMARK = builder.comment("Skip Xaero's GPU-synchronizing texture upload timing benchmark.",
                "Uploads still run normally. Unmeasured timings retain Xaero's original per-type upload budget estimates.",
                "These estimates affect actual upload scheduling, not only debug statistics.")
                .define("skip_upload_benchmark", true);
        DEDUPLICATE_LAYER_MARKS = builder.comment("Avoid marking the same region's cave layers repeatedly in one map writer pass.")
                .define("deduplicate_layer_marks", true);
        VRAM_POLL_INTERVAL = builder.comment("Minimum milliseconds between free-VRAM queries. 0 restores every-frame polling.")
                .defineInRange("vram_poll_interval_ms", 500, 0, 60000);
        ORPHAN_UPLOAD_PBO = builder.comment("Discard old upload-buffer storage before mapping a complete replacement.",
                "Uses Xaero's own OpenGL/ARB wrapper and its existing failure fallback.",
                "Disable if your graphics driver exhibits map upload issues.")
                .define("orphan_upload_pbo", true);
        BATCH_OVERLAY = builder.comment("Batch FRMC's chunk-pressure overlay with Accelerated Rendering's GUI pipeline when available.",
                "Respects AR's GUI batching options. Does not replace Xaero's own multi-texture renderer.")
                .define("ar_batch_pressure_overlay", true);
        SPEC = builder.build();
    }

    private XaeroPerformanceConfig() {
    }

    public static boolean lazyHeights() {
        return SPEC.isLoaded() && LAZY_HEIGHTS.get();
    }

    public static boolean skipUploadBenchmark() {
        return SPEC.isLoaded() && SKIP_UPLOAD_BENCHMARK.get();
    }

    public static boolean deduplicateLayerMarks() {
        return SPEC.isLoaded() && DEDUPLICATE_LAYER_MARKS.get();
    }

    public static int vramPollIntervalMillis() {
        return SPEC.isLoaded() ? VRAM_POLL_INTERVAL.get() : 0;
    }

    public static boolean orphanUploadPbo() {
        return SPEC.isLoaded() && ORPHAN_UPLOAD_PBO.get();
    }

    public static boolean batchOverlay() {
        return SPEC.isLoaded() && BATCH_OVERLAY.get();
    }

    @SuppressWarnings("removal")
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "frmc/frmc-xaero.toml");
    }
}
