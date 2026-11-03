package com.y271727uy.FRMC.integration.acceleratedrendering.citadel;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class CitadelEmbeddiumConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DIRECT_WRITER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        DIRECT_WRITER = builder
            .comment(
                "Write Citadel model geometry through Embeddium's VertexBufferWriter when Accelerated Rendering is not handling it.",
                "Disable this to fall back to Citadel's own vertex submission."
            )
            .define("direct_writer", true);
        SPEC = builder.build();
    }

    private CitadelEmbeddiumConfig() {
    }

    public static boolean isDirectWriterEnabled() {
        return SPEC.isLoaded() && DIRECT_WRITER.get();
    }

    @SuppressWarnings("removal")
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "frmc/frmc-citadel-embeddium.toml");
    }
}
