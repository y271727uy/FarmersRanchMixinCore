package com.y271727uy.FRMC.integration.netmusic;

import com.y271727uy.FRMC.config.OnlineMusicConfig;
import net.minecraftforge.fml.ModList;

/** Common, dependency-safe entry point for the optional NetMusic integration. */
public final class NetMusicIntegration {
    public static final String MOD_ID = "netmusic";

    private NetMusicIntegration() {}

    public static boolean isAvailable() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isEnabled() {
        return isAvailable() && OnlineMusicConfig.ENABLED.get();
    }

    public static String songUrl(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("NetEase song id must be positive");
        }
        return "https://music.163.com/song/media/outer/url?id=" + id + ".mp3";
    }
}
