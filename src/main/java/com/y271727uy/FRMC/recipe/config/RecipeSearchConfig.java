package com.y271727uy.FRMC.recipe.config;

import com.y271727uy.FRMC.Config;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RecipeSearchConfig {
    static final Path CONFIG_PATH = Paths.get("config", "frmc-recipe-search.properties");
    private static final Logger LOGGER = Logger.getLogger(RecipeSearchConfig.class.getName());
    private static volatile boolean loaded;

    private RecipeSearchConfig() {
    }

    public static void load() {
        if (loaded) {
            return;
        }
        synchronized (RecipeSearchConfig.class) {
            if (loaded) {
                return;
            }
            Properties properties = defaultProperties();
            try {
                Path directory = CONFIG_PATH.getParent();
                if (directory != null) {
                    Files.createDirectories(directory);
                }
                if (Files.exists(CONFIG_PATH)) {
                    try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
                        properties.load(input);
                    }
                } else {
                    writeDefaults(properties);
                }
                apply(properties);
            } catch (IOException exception) {
                LOGGER.log(Level.SEVERE, "Failed to load recipe search config from " + CONFIG_PATH.toAbsolutePath(), exception);
                apply(defaultProperties());
            }
            loaded = true;
        }
    }

    static void apply(Properties properties) {
        Config.recipeSearchEnabled = booleanProperty(properties, "enable", true);
        Config.recipeSearchOptimizeOnlyVanilla = booleanProperty(properties, "optimize_only_vanilla", true);
        Config.recipeIngredientSyncEnabled = booleanProperty(properties, "ingredient_sync", false);
        Config.recipeIngredientDeduplicatorEnabled = booleanProperty(properties, "ingredient_deduplicator", false);
    }

    private static boolean booleanProperty(Properties properties, String key, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(defaultValue)).trim());
    }

    private static Properties defaultProperties() {
        Properties properties = new Properties();
        properties.setProperty("enable", "true");
        properties.setProperty("optimize_only_vanilla", "true");
        properties.setProperty("ingredient_sync", "false");
        properties.setProperty("ingredient_deduplicator", "false");
        return properties;
    }

    private static void writeDefaults(Properties properties) throws IOException {
        try (OutputStream output = Files.newOutputStream(CONFIG_PATH)) {
            properties.store(output, "FRMC Fast-Recipe-Search compatibility settings");
        }
    }
}
