package com.y271727uy.FRMC.capability.data_group;

import com.y271727uy.FRMC.capability.data_group.provider.DataProviderRegistry;
import com.y271727uy.FRMC.capability.data_group.provider.ForestryDataProvider;
import com.y271727uy.FRMC.integration.sereneseasons.SeasonalGroupMapping;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

/**
 * Initializes the data group system.
 */
public final class DataGroupInit {
    private DataGroupInit() {
    }

    /**
     * Registers data providers and reload listeners.
     */
    public static void init() {
        // Register built-in providers
        DataProviderRegistry.register(new ForestryDataProvider());

        // Register reload listener
        MinecraftForge.EVENT_BUS.register(DataGroupInit.class);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new DataGroupReloadListener());
        
        // Register seasonal mapping listener if Serene Seasons is loaded
        if (ModList.get().isLoaded("sereneseasons")) {
            event.addListener(new SeasonalGroupMapping.ReloadListener());
        }
    }
}
