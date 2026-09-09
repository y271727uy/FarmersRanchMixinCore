package com.y271727uy.FRMC.client.entityactivity;

import com.y271727uy.FRMC.FRMCMod;
import com.y271727uy.FRMC.network.EntityActivityNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/** Collects Chloride's final render decisions and sends only visibility changes. */
@Mod.EventBusSubscriber(modid = FRMCMod.MODID, value = Dist.CLIENT)
public final class ChlorideVisibilityBridge {
    private static final int MAX_DELTAS_PER_PACKET = 128;
    private static final int BACKGROUND_FLUSH_INTERVAL_TICKS = 5;
    private static final Map<Integer, Boolean> LAST_STATE = new HashMap<>();
    private static final Map<Integer, Boolean> PENDING = new HashMap<>();
    private static int flushCooldown;

    private ChlorideVisibilityBridge() {
    }

    public static void observe(Entity entity, boolean visible) {
        if (!ModList.get().isLoaded("chloride")) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || entity == minecraft.player
                || entity.level() != minecraft.level) {
            return;
        }
        int entityId = entity.getId();
        Boolean previous = LAST_STATE.put(entityId, visible);
        if (previous == null || previous != visible) {
            PENDING.put(entityId, visible);
        }
    }

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        LAST_STATE.clear();
        PENDING.clear();
        flushCooldown = 0;
        boolean hasChloride = ModList.get().isLoaded("chloride");
        EntityActivityNetwork.CHANNEL.sendToServer(
                new EntityActivityNetwork.ClientCapabilitiesPacket(
                        EntityActivityNetwork.CLIENT_PROTOCOL_VERSION, hasChloride, hasChloride));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        LAST_STATE.clear();
        PENDING.clear();
        flushCooldown = 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
            return;
        }
        boolean hasWakeup = PENDING.containsValue(Boolean.TRUE);
        if (!hasWakeup && flushCooldown > 0) {
            flushCooldown--;
            return;
        }

        long[] states = new long[Math.min(PENDING.size(), MAX_DELTAS_PER_PACKET)];
        int count = 0;
        var iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext() && count < states.length) {
            var entry = iterator.next();
            states[count++] = ((long) entry.getKey() << 1) | (entry.getValue() ? 1L : 0L);
            iterator.remove();
        }
        EntityActivityNetwork.CHANNEL.sendToServer(new EntityActivityNetwork.EntityVisibilityBatchPacket(states));
        flushCooldown = BACKGROUND_FLUSH_INTERVAL_TICKS - 1;
    }
}
