package com.y271727uy.FRMC.integration.untamedwilds;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class UntamedWildsFriendlyFireConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue PREVENT_FRIENDLY_FIRE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        PREVENT_FRIENDLY_FIRE = builder
                .comment(
                        "Prevent Untamed Wilds ComplexMob entities from damaging one another.",
                        "Keep false for normal Untamed Wilds combat; enable for stable rendering benchmarks."
                )
                .define("prevent_friendly_fire", false);
        SPEC = builder.build();
    }

    private UntamedWildsFriendlyFireConfig() {
    }

    public static boolean isEnabled() {
        return SPEC.isLoaded() && PREVENT_FRIENDLY_FIRE.get();
    }

    @SuppressWarnings("removal")
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "frmc/untamedwilds.toml");
    }
}
