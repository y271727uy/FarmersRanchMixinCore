package com.y271727uy.FRMC.integration.sereneseasons;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.server.level.ServerLevel;

import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.Season.SubSeason;
import sereneseasons.config.SeasonsConfig.SeasonProperties;
import sereneseasons.config.SeasonsConfig;
import sereneseasons.init.ModConfig;
import sereneseasons.api.season.SeasonHelper;

/** One season/config snapshot per server world and game tick. */
public final class SeasonSnapshotCache {
    private static final ConcurrentMap<ServerLevel, Entry> ENTRIES = new ConcurrentHashMap<>();

    private SeasonSnapshotCache() {
    }

    public static Snapshot get(ServerLevel level) {
        long gameTime = level.getGameTime();
        Entry entry = ENTRIES.get(level);
        if (entry != null && entry.gameTime() == gameTime) {
            return entry.snapshot();
        }

        ISeasonState state = SeasonHelper.getSeasonState(level);
        SubSeason subSeason = state.getSubSeason();
        SeasonsConfig config = ModConfig.seasons;
        SeasonProperties properties = config.getSeasonProperties(subSeason);
        Snapshot snapshot = new Snapshot(
            gameTime,
            state.getSeasonCycleTicks(),
            subSeason,
            properties,
            config.generateSnowAndIce,
            config.isDimensionWhitelisted(level.dimension())
        );
        ENTRIES.put(level, new Entry(gameTime, snapshot));
        return snapshot;
    }

    public static void invalidate(ServerLevel level) {
        ENTRIES.remove(level);
    }

    public record Snapshot(
        long gameTime,
        int seasonCycleTicks,
        SubSeason subSeason,
        SeasonProperties properties,
        boolean generateSnowAndIce,
        boolean dimensionWhitelisted
    ) {
    }

    private record Entry(long gameTime, Snapshot snapshot) {
    }
}
