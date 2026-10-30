package com.y271727uy.FRMC.integration.sereneseasons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import sereneseasons.api.season.Season;

import java.util.*;

/**
 * Maps data groups to seasonal growth rules.
 * Allows data groups to be recognized as seasonal crops by Serene Seasons.
 */
public final class SeasonalGroupMapping {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Group -> Allowed Seasons
    private static final Map<ResourceLocation, Set<Season.SubSeason>> GROUP_SEASONS = new HashMap<>();
    
    private SeasonalGroupMapping() {
    }

    /**
     * Checks if a data group is allowed in the current season.
     */
    public static boolean isAllowedInSeason(ResourceLocation groupId, Season.SubSeason season) {
        Set<Season.SubSeason> allowed = GROUP_SEASONS.get(groupId);
        if (allowed == null) {
            // If not configured, allow in all seasons by default
            return true;
        }
        return allowed.contains(season);
    }

    /**
     * Registers a seasonal mapping.
     */
    public static void register(ResourceLocation groupId, Set<Season.SubSeason> seasons) {
        GROUP_SEASONS.put(groupId, new HashSet<>(seasons));
    }

    /**
     * Clears all mappings. Used during reload.
     */
    public static void clear() {
        GROUP_SEASONS.clear();
    }

    /**
     * Returns all groups allowed in the given season.
     */
    public static Set<ResourceLocation> getGroupsForSeason(Season.SubSeason season) {
        Set<ResourceLocation> result = new HashSet<>();
        for (Map.Entry<ResourceLocation, Set<Season.SubSeason>> entry : GROUP_SEASONS.entrySet()) {
            if (entry.getValue().contains(season)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Reload listener for seasonal group mappings.
     */
    public static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
        private static final String DIRECTORY = "seasonal_groups";

        public ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
            SeasonalGroupMapping.clear();
            int loaded = 0;

            for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
                ResourceLocation id = entry.getKey();
                JsonElement json = entry.getValue();

                try {
                    if (!json.isJsonObject()) {
                        LOGGER.warn("Seasonal group mapping {} is not a JSON object, skipping", id);
                        continue;
                    }

                    parseMapping(id, json.getAsJsonObject());
                    loaded++;

                } catch (Exception e) {
                    LOGGER.error("Failed to load seasonal group mapping {}", id, e);
                }
            }

            LOGGER.info("Loaded {} seasonal group mapping(s)", loaded);
        }

        private void parseMapping(ResourceLocation id, JsonObject json) {
            if (!json.has("group")) {
                LOGGER.warn("Seasonal mapping {} is missing 'group' field", id);
                return;
            }

            String groupIdStr = json.get("group").getAsString();
            ResourceLocation groupId = groupIdStr.startsWith("#") 
                ? new ResourceLocation(groupIdStr.substring(1))
                : new ResourceLocation(groupIdStr);

            Set<Season.SubSeason> seasons = new HashSet<>();

            if (json.has("allowed_seasons")) {
                for (JsonElement element : json.getAsJsonArray("allowed_seasons")) {
                    String seasonStr = element.getAsString().toUpperCase();
                    try {
                        // Parse season name like "early_spring", "spring", "summer"
                        Season.SubSeason season = Season.SubSeason.valueOf(seasonStr);
                        seasons.add(season);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Unknown season '{}' in mapping {}", seasonStr, id);
                    }
                }
            } else {
                // If no seasons specified, allow all seasons
                seasons.addAll(Arrays.asList(Season.SubSeason.values()));
            }

            SeasonalGroupMapping.register(groupId, seasons);
            LOGGER.debug("Registered seasonal mapping: {} -> {}", groupId, seasons);
        }
    }
}
