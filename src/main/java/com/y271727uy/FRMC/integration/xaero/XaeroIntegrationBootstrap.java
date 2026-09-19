package com.y271727uy.FRMC.integration.xaero;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

/** Initializes each optional Xaero module independently during client setup. */
public final class XaeroIntegrationBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private XaeroIntegrationBootstrap() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("FRMC Xaero integration bootstrap starting");
        if (isLoaded("xaeroworldmap")) {
            LOGGER.info("Xaero World Map detected, initializing FRMC integration");
            XaeroWorldMapIntegration.initialize(true);
            LOGGER.info("FRMC Xaero World Map integration initialized");
        } else {
            LOGGER.warn("Xaero World Map not found, skipping integration");
        }
        // FRMC intentionally integrates only with the full-screen World Map.
    }

    private static boolean isLoaded(String modId) {
        try {
            return ModList.get().isLoaded(modId);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
