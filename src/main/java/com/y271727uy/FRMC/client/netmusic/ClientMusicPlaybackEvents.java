package com.y271727uy.FRMC.client.netmusic;

import com.y271727uy.FRMC.FRMCMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FRMCMod.MODID, value = Dist.CLIENT)
public final class ClientMusicPlaybackEvents {
    private ClientMusicPlaybackEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientMusicPlaybackManager.getInstance().tick();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMusicPlaybackManager.getInstance().clear();
    }
}
