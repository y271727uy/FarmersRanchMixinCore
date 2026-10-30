package com.y271727uy.FRMC.capability.data_group.provider;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Registry for data providers.
 */
public final class DataProviderRegistry {
    private static final Map<String, DataProvider> PROVIDERS = new HashMap<>();

    private DataProviderRegistry() {
    }

    /**
     * Registers a data provider.
     */
    public static void register(DataProvider provider) {
        PROVIDERS.put(provider.getId(), provider);
    }

    /**
     * Gets a provider by ID.
     */
    @Nullable
    public static DataProvider get(String id) {
        return PROVIDERS.get(id);
    }

    /**
     * Checks if a provider exists.
     */
    public static boolean contains(String id) {
        return PROVIDERS.containsKey(id);
    }

    /**
     * Clears all providers.
     */
    public static void clear() {
        PROVIDERS.clear();
    }
}
