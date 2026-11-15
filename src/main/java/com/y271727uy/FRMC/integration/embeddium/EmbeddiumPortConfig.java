package com.y271727uy.FRMC.integration.embeddium;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** Emberbium 新增功能移植到原版 Embeddium 的开关，每项功能一个键。 */
public final class EmbeddiumPortConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue GUI_ANIMATIONS;
    public static final ForgeConfigSpec.BooleanValue CHUNK_FADE_IN;
    public static final ForgeConfigSpec.BooleanValue MESH_BUFFER_GROWTH_FIX;
    public static final ForgeConfigSpec.BooleanValue RADIX_VERTEX_SORT;
    public static final ForgeConfigSpec.BooleanValue BLOCK_ENTITY_BOX_CACHE;
    public static final ForgeConfigSpec.BooleanValue SPRITE_MARKING_PER_TICK;
    public static final ForgeConfigSpec.BooleanValue BLOCK_TINT_CACHE;
    public static final ForgeConfigSpec.IntValue SHADOW_UPDATE_INTERVAL;
    public static final ForgeConfigSpec.IntValue CHUNK_UPLOAD_BUDGET_KB;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        GUI_ANIMATIONS = builder
            .comment(
                "Animate Embeddium's video settings screen: hover fades, tick boxes, sliders, tab switches and scrolling.",
                "Ported from the Emberbium fork. Purely cosmetic; disable to get Embeddium's stock widgets back."
            )
            .define("gui_animations", true);
        CHUNK_FADE_IN = builder
            .comment(
                "Newly loaded chunks fade in out of the fog over 0.75s instead of popping in.",
                "Ported from the Emberbium fork. Regions that are not fading render exactly as before.",
                "Has no effect while a shader pack is active."
            )
            .define("chunk_fade_in", true);
        MESH_BUFFER_GROWTH_FIX = builder
            .comment(
                "Fix Embeddium 0.3.31 multiplying the chunk mesh buffer size by the vertex stride twice when it grows.",
                "Without the fix, a dense chunk section that outgrows its initial buffer allocates dozens of times more native memory than it needs."
            )
            .define("mesh_buffer_growth_fix", true);
        RADIX_VERTEX_SORT = builder
            .comment(
                "Sort immediate-mode translucent buffers of 256+ quads with a radix sort instead of a merge sort.",
                "Produces the same order. Only affects layers sorted through the vanilla BufferBuilder, not chunk terrain."
            )
            .define("radix_vertex_sort", true);
        BLOCK_ENTITY_BOX_CACHE = builder
            .comment(
                "Cache the render bounding box of block entities that use the default Forge implementation,",
                "instead of recomputing it from the collision shape for every visible block entity every frame.",
                "Block entities that override getRenderBoundingBox, and blocks with dynamic shapes, are never cached."
            )
            .define("block_entity_box_cache", true);
        SPRITE_MARKING_PER_TICK = builder
            .comment(
                "Mark the animated textures of visible chunk sections as active once per client tick instead of once per frame.",
                "Texture animations only advance on ticks, so the per-frame pass is redundant above 20 fps.",
                "Only takes over while Embeddium's 'Animate Only Visible Textures' is enabled. Ported from the Emberbium fork."
            )
            .define("sprite_marking_per_tick", true);
        BLOCK_TINT_CACHE = builder
            .comment(
                "While meshing a chunk, query a block's vanilla BlockColor once per tint index per model instead of once per quad.",
                "Biome-blended colors (grass, foliage, water) and color providers registered by other mods are never cached."
            )
            .define("block_tint_cache", true);
        SHADOW_UPDATE_INTERVAL = builder
            .comment(
                "With an Oculus shader pack active, render the shadow map once every N frames (1 = every frame).",
                "Skipped frames reuse the previous shadow map, shifted to follow the camera.",
                "Shadows of moving entities and of the sun update at the lower rate.",
                "Packs that clear custom images every frame or use shadow compute shaders always render every frame."
            )
            .defineInRange("shadow_update_interval", 1, 1, 4);
        CHUNK_UPLOAD_BUDGET_KB = builder
            .comment(
                "Upload at most this many KiB of finished chunk meshes per frame; the rest waits for the next frame (0 = no limit, Embeddium's stock behaviour).",
                "Smooths frame time spikes when many chunk sections finish building at once, at the cost of those sections appearing a few frames later.",
                "Block changes made by the player and forced immediate updates are never delayed, and the limit is lifted while more than 64 results are waiting."
            )
            .defineInRange("chunk_upload_budget_kb", 0, 0, 4096);
        SPEC = builder.build();
    }

    private EmbeddiumPortConfig() {
    }

    public static boolean guiAnimations() {
        return SPEC.isLoaded() && GUI_ANIMATIONS.get();
    }

    public static boolean chunkFadeIn() {
        return SPEC.isLoaded() && CHUNK_FADE_IN.get();
    }

    public static boolean meshBufferGrowthFix() {
        return SPEC.isLoaded() && MESH_BUFFER_GROWTH_FIX.get();
    }

    public static boolean radixVertexSort() {
        return SPEC.isLoaded() && RADIX_VERTEX_SORT.get();
    }

    public static boolean blockEntityBoxCache() {
        return SPEC.isLoaded() && BLOCK_ENTITY_BOX_CACHE.get();
    }

    public static boolean spriteMarkingPerTick() {
        return SPEC.isLoaded() && SPRITE_MARKING_PER_TICK.get();
    }

    public static boolean blockTintCache() {
        return SPEC.isLoaded() && BLOCK_TINT_CACHE.get();
    }

    public static int shadowUpdateInterval() {
        return SPEC.isLoaded() ? SHADOW_UPDATE_INTERVAL.get() : 1;
    }

    public static int chunkUploadBudgetKb() {
        return SPEC.isLoaded() ? CHUNK_UPLOAD_BUDGET_KB.get() : 0;
    }

    @SuppressWarnings("removal")
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "frmc/frmc-embeddium.toml");
    }
}
