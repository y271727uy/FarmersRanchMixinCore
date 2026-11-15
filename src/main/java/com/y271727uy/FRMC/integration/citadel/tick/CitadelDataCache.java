package com.y271727uy.FRMC.integration.citadel.tick;

import com.github.alexthe666.citadel.server.tick.ServerTickRateTracker;
import com.github.alexthe666.citadel.server.world.CitadelServerData;
import net.minecraft.server.MinecraftServer;

/** 单槽缓存：避免每个实体每 tick 都走 DimensionDataStorage.computeIfAbsent。 */
public final class CitadelDataCache {
    private record Entry(MinecraftServer server, CitadelServerData data) {}

    private static volatile Entry cache;

    private CitadelDataCache() {}

    public static CitadelServerData lookup(MinecraftServer server) {
        Entry e = cache;
        return e != null && e.server == server ? e.data : null;
    }

    public static void store(MinecraftServer server, CitadelServerData data) {
        Entry e = cache;
        if (e == null || e.server != server || e.data != data) {
            cache = new Entry(server, data);
        }
    }

    public static ServerTickRateTracker trackerFor(MinecraftServer server) {
        CitadelServerData data = lookup(server);
        return data != null ? data.getOrCreateTickRateTracker() : ServerTickRateTracker.getForServer(server);
    }

    public static ServerTickRateTracker existingTracker(MinecraftServer server) {
        CitadelServerData data = lookup(server);
        return data == null ? null : data.getOrCreateTickRateTracker();
    }

    public static void invalidate() {
        cache = null;
    }
}
