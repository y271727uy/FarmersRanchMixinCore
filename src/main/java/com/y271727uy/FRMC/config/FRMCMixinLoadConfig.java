package com.y271727uy.FRMC.config;

import net.minecraftforge.fml.loading.FMLLoader;

/** Runtime availability checks for the optional Starlight/Create compatibility layer. */
public final class FRMCMixinLoadConfig {
    private FRMCMixinLoadConfig() {
    }

    public static boolean isEnabled() {
        try {
            var loadingModList = FMLLoader.getLoadingModList();
            return loadingModList.getModFileById("starlight") != null
                && loadingModList.getModFileById("create") != null;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
