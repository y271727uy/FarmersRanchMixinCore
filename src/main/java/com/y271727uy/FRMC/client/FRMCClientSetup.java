package com.y271727uy.FRMC.client;

import com.y271727uy.FRMC.capability.downland.ContentPackRegistry;
import com.y271727uy.FRMC.integration.xaero.XaeroIntegrationBootstrap;
import com.y271727uy.FRMC.capability.blockchecking.client.ClientBlockCheckingNetworkHandler;
import net.minecraftforge.eventbus.api.IEventBus;

/** Client-only setup entry point kept behind DistExecutor in the common mod class. */
public final class FRMCClientSetup {
    private FRMCClientSetup() {
    }

    public static void register(IEventBus modEventBus) {
        ContentPackRegistry.load();
        ClientBlockCheckingNetworkHandler.register();
        modEventBus.addListener(XaeroIntegrationBootstrap::onClientSetup);
    }
}
