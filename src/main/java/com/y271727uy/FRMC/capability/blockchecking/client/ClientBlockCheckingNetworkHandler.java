package com.y271727uy.FRMC.capability.blockchecking.client;

import com.y271727uy.FRMC.network.BlockCheckingNetwork;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

/** Client-only packet bridge; this class is never referenced by common setup directly. */
public final class ClientBlockCheckingNetworkHandler {
    public static final ClientBlockCheckingState STATE = new ClientBlockCheckingState();

    private ClientBlockCheckingNetworkHandler() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(ClientBlockCheckingNetworkHandler.class);
    }

    public static void handle(BlockCheckingNetwork.ChunkSnapshotPacket packet) {
        STATE.updateSnapshot(packet.revision(), packet.timestamp(), packet.snapshot());
    }

    public static void handle(BlockCheckingNetwork.BlockCheckingReportPacket packet) {
        STATE.updateReport(packet.revision(), packet.timestamp(), packet.report());
    }

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        BlockCheckingNetwork.requestSubscription();
    }

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        STATE.clear();
    }
}
