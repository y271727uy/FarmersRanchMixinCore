package com.y271727uy.FRMC.client.modelgap;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class ModelGapFixConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue ITEM_QUADS_EXPANSION;
    public static final ForgeConfigSpec.DoubleValue ITEM_QUADS_INDENT;
    public static final ForgeConfigSpec.DoubleValue MAC_ITEM_QUADS_EXPANSION;
    public static final ForgeConfigSpec.DoubleValue MAC_ITEM_QUADS_INDENT;
    public static final ForgeConfigSpec.DoubleValue MAC_SHRINK_RATIO_MULTIPLIER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        ENABLED = builder
            .comment("Enable the item and block model gap fix.")
            .define("enabled", true);

        builder.push("default");
        ITEM_QUADS_EXPANSION = builder
            .comment(
                "Amount added around generated item quads to hide gaps.",
                "Keep this as close to zero as possible; useful values include 0.002 and 0.008."
            )
            .defineInRange("item_quads_expansion", 0.0D, -0.1D, 0.1D);
        ITEM_QUADS_INDENT = builder
            .comment("Offset generated item quads toward the center of the item.")
            .defineInRange("item_quads_indent", 0.007D, -0.1D, 0.1D);
        builder.pop();

        builder.push("mac_os");
        MAC_ITEM_QUADS_EXPANSION = builder
            .comment("Amount added around generated item quads on macOS.")
            .defineInRange("item_quads_expansion", 0.0D, -0.1D, 0.1D);
        MAC_ITEM_QUADS_INDENT = builder
            .comment("Offset generated item quads toward the center of the item on macOS.")
            .defineInRange("item_quads_indent", 0.0099D, -0.1D, 0.1D);
        MAC_SHRINK_RATIO_MULTIPLIER = builder
            .comment(
                "Multiplier for vanilla atlas shrinking on macOS.",
                "Zero matches the non-macOS fix; one keeps vanilla behavior to avoid atlas bleeding."
            )
            .defineInRange("shrink_ratio_multiplier", 1.0D, 0.0D, 1.0D);
        builder.pop();

        SPEC = builder.build();
    }

    private ModelGapFixConfig() {
    }

    @SuppressWarnings("removal")
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "frmc/frmc-model-gap-fix.toml");
    }
}
