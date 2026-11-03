package com.y271727uy.FRMC.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class UntamedWildsAnimationConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ENABLED = builder
                .comment(
                        "Enable low-risk Untamed Wilds animation math caching.",
                        "This only reuses identical bob calculations within one model animation pass."
                )
                .define("enabled", true);
        SPEC = builder.build();
    }

    private UntamedWildsAnimationConfig() {
    }

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    @SuppressWarnings("removal")
    public static void register() {
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT,
                SPEC,
                "frmc/frmc-untamed-wilds-animation.toml"
        );
    }
}
