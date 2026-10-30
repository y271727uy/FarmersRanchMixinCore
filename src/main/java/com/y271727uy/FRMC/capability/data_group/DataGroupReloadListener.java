package com.y271727uy.FRMC.capability.data_group;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.FRMCMod;
import com.y271727uy.FRMC.capability.data_group.matcher.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reloads data groups from data packs.
 */
public final class DataGroupReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "data_groups";

    public DataGroupReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        DataGroupRegistry.clear();
        int loaded = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                if (!json.isJsonObject()) {
                    LOGGER.warn("Data group {} is not a JSON object, skipping", id);
                    continue;
                }

                DataGroup group = parseDataGroup(id, json.getAsJsonObject());
                DataGroupRegistry.register(group);
                loaded++;

            } catch (Exception e) {
                LOGGER.error("Failed to load data group {}", id, e);
            }
        }

        LOGGER.info("Loaded {} data group(s)", loaded);
    }

    private DataGroup parseDataGroup(ResourceLocation id, JsonObject json) {
        boolean replace = json.has("replace") && json.get("replace").getAsBoolean();
        
        DataGroup.Builder builder = DataGroup.builder(id).replace(replace);

        if (json.has("values")) {
            JsonArray values = json.getAsJsonArray("values");
            for (JsonElement element : values) {
                GroupMatcher matcher = parseMatcher(element);
                if (matcher != null) {
                    builder.add(matcher);
                }
            }
        }

        return builder.build();
    }

    private GroupMatcher parseMatcher(JsonElement element) {
        if (element.isJsonPrimitive()) {
            // Simple string: item or block ID
            String value = element.getAsString();
            if (value.startsWith("#")) {
                // Tag reference - default to item tag
                ResourceLocation tagId = new ResourceLocation(value.substring(1));
                return new ItemTagMatcher(TagKey.create(Registries.ITEM, tagId));
            } else {
                // Item ID
                return new ItemIdMatcher(new ResourceLocation(value));
            }
        }

        if (!element.isJsonObject()) {
            LOGGER.warn("Matcher is not a JSON object or string: {}", element);
            return null;
        }

        JsonObject obj = element.getAsJsonObject();
        String type = obj.has("type") ? obj.get("type").getAsString() : "item_id";

        try {
            return switch (type) {
                case "item_id" -> parseItemIdMatcher(obj);
                case "item_tag" -> parseItemTagMatcher(obj);
                case "item_nbt" -> parseItemNBTMatcher(obj);
                case "item_provider" -> parseItemProviderMatcher(obj);
                case "block_id" -> parseBlockIdMatcher(obj);
                case "block_tag" -> parseBlockTagMatcher(obj);
                case "block_provider" -> parseBlockProviderMatcher(obj);
                default -> {
                    LOGGER.warn("Unknown matcher type: {}", type);
                    yield null;
                }
            };
        } catch (Exception e) {
            LOGGER.error("Failed to parse matcher of type {}", type, e);
            return null;
        }
    }

    private ItemIdMatcher parseItemIdMatcher(JsonObject obj) {
        ResourceLocation itemId = new ResourceLocation(obj.get("item").getAsString());
        return new ItemIdMatcher(itemId);
    }

    private ItemTagMatcher parseItemTagMatcher(JsonObject obj) {
        ResourceLocation tagId = new ResourceLocation(obj.get("tag").getAsString());
        return new ItemTagMatcher(TagKey.create(Registries.ITEM, tagId));
    }

    private ItemNBTMatcher parseItemNBTMatcher(JsonObject obj) {
        ResourceLocation itemId = new ResourceLocation(obj.get("item").getAsString());
        String path = obj.get("path").getAsString();
        String value = obj.has("value") ? obj.get("value").getAsString() : null;
        return new ItemNBTMatcher(itemId, path, value);
    }

    private ItemProviderMatcher parseItemProviderMatcher(JsonObject obj) {
        ResourceLocation itemId = new ResourceLocation(obj.get("item").getAsString());
        String provider = obj.get("provider").getAsString();
        String field = obj.get("field").getAsString();
        String value = obj.has("value") ? obj.get("value").getAsString() : null;
        return new ItemProviderMatcher(itemId, provider, field, value);
    }

    private BlockIdMatcher parseBlockIdMatcher(JsonObject obj) {
        ResourceLocation blockId = new ResourceLocation(obj.get("block").getAsString());
        return new BlockIdMatcher(blockId);
    }

    private BlockTagMatcher parseBlockTagMatcher(JsonObject obj) {
        ResourceLocation tagId = new ResourceLocation(obj.get("tag").getAsString());
        return new BlockTagMatcher(TagKey.create(Registries.BLOCK, tagId));
    }

    private BlockProviderMatcher parseBlockProviderMatcher(JsonObject obj) {
        ResourceLocation blockId = new ResourceLocation(obj.get("block").getAsString());
        String provider = obj.get("provider").getAsString();
        String field = obj.get("field").getAsString();
        String value = obj.has("value") ? obj.get("value").getAsString() : null;
        return new BlockProviderMatcher(blockId, provider, field, value);
    }
}
