package com.y271727uy.FRMC.client;

import com.y271727uy.FRMC.capability.downland.ContentPackRegistry;
import com.y271727uy.FRMC.integration.xaero.XaeroIntegrationBootstrap;
import com.y271727uy.FRMC.capability.blockchecking.client.ClientBlockCheckingNetworkHandler;
import com.y271727uy.FRMC.integration.embeddium.EmbeddiumPortOptions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

/** Client-only setup entry point kept behind DistExecutor in the common mod class. */
public final class FRMCClientSetup {
    private FRMCClientSetup() {
    }

    public static void register(IEventBus modEventBus) {
        ContentPackRegistry.load();
        ClientBlockCheckingNetworkHandler.register();
        modEventBus.addListener(XaeroIntegrationBootstrap::onClientSetup);
        if (ModList.get().isLoaded("embeddium")) {
            EmbeddiumPortOptions.register();
        }
    }
}
