package com.y271727uy.FRMC.integration.citadel.tick;

import com.github.alexthe666.citadel.client.tick.ClientTickRateTracker;
import net.minecraft.client.Minecraft;

public final class ClientTrackerCache {
    private record Entry(Minecraft client, ClientTickRateTracker tracker) {}

    private static volatile Entry cache;

    private ClientTrackerCache() {}

    public static ClientTickRateTracker lookup(Minecraft client) {
        Entry e = cache;
        return e != null && e.client == client ? e.tracker : null;
    }

    public static void store(Minecraft client, ClientTickRateTracker tracker) {
        Entry e = cache;
        if (e == null || e.client != client || e.tracker != tracker) {
            cache = new Entry(client, tracker);
        }
    }

    public static ClientTickRateTracker existing() {
        Entry e = cache;
        return e == null ? null : e.tracker;
    }
}
