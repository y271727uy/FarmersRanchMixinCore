package com.y271727uy.FRMC.capability.data_group;

import com.y271727uy.FRMC.capability.data_group.target.MatchTarget;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry for all data groups.
 * Similar to tag managers but for dynamic field-based groups.
 */
public final class DataGroupRegistry {
    private static final Map<ResourceLocation, DataGroup> GROUPS = new HashMap<>();

    private DataGroupRegistry() {
    }

    /**
     * Registers a data group.
     */
    public static void register(DataGroup group) {
        GROUPS.put(group.getId(), group);
    }

    /**
     * Gets a data group by ID.
     */
    public static DataGroup get(ResourceLocation id) {
        return GROUPS.get(id);
    }

    /**
     * Tests if a target belongs to the specified group.
     */
    public static boolean test(ResourceLocation groupId, MatchTarget target) {
        DataGroup group = GROUPS.get(groupId);
        return group != null && group.test(target);
    }

    /**
     * Returns all group IDs that match the given target.
     */
    public static Set<ResourceLocation> getMatchingGroups(MatchTarget target) {
        return GROUPS.values().stream()
            .filter(group -> group.test(target))
            .map(DataGroup::getId)
            .collect(Collectors.toSet());
    }

    /**
     * Returns all registered groups.
     */
    public static Collection<DataGroup> getAll() {
        return GROUPS.values();
    }

    /**
     * Clears all groups. Used during reload.
     */
    public static void clear() {
        GROUPS.clear();
    }

    /**
     * Returns the number of registered groups.
     */
    public static int size() {
        return GROUPS.size();
    }

    /**
     * Checks if a group exists.
     */
    public static boolean contains(ResourceLocation id) {
        return GROUPS.containsKey(id);
    }
}
