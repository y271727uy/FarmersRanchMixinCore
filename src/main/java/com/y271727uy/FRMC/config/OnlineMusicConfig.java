package com.y271727uy.FRMC.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class OnlineMusicConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.EnumValue<PlaybackMode> PLAYBACK_MODE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("online_music");
        ENABLED = builder.comment("Enable the optional NetMusic online music integration.")
                .define("enabled", true);
        PLAYBACK_MODE = builder.comment("Playlist playback mode.")
                .defineEnum("playback_mode", PlaybackMode.LIST_LOOP);
        builder.pop();
        SPEC = builder.build();
    }

    private OnlineMusicConfig() {}

    public enum PlaybackMode {
        LIST_LOOP, SINGLE_LOOP, RANDOM
    }
}
