package com.y271727uy.FRMC.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/** Pack-specific natural spawn-rate controls for high-density fauna mods. */
public final class MobSpawnRateConfig {
    private static final Path CONFIG_PATH = FRMCConfigPaths.resolve("frmc-mob-spawn-rate.properties");

    private static volatile double alexsMobsWeightMultiplier = 0.25D;
    private static volatile double aquacultureWeightMultiplier = 0.25D;
    private static volatile double unusualFishWeightMultiplier = 0.25D;

    private MobSpawnRateConfig() {
    }

    public static void load() {
        Properties properties = defaults();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.exists(CONFIG_PATH)) {
                try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
                    properties.load(input);
                }
            } else {
                try (OutputStream output = Files.newOutputStream(CONFIG_PATH)) {
                    properties.store(output, "FRMC pack-specific natural spawn-rate settings");
                }
            }
        } catch (IOException ignored) {
            // Keep the in-memory defaults when the config cannot be read or created.
        }
        alexsMobsWeightMultiplier = fraction(properties, "alexsmobs_weight_multiplier", 0.25D);
        aquacultureWeightMultiplier = fraction(properties, "aquaculture_weight_multiplier", 0.25D);
        unusualFishWeightMultiplier = fraction(properties, "unusualfishmod_weight_multiplier", 0.25D);
    }

    public static double alexsMobsWeightMultiplier() {
        return alexsMobsWeightMultiplier;
    }

    public static double aquacultureWeightMultiplier() {
        return aquacultureWeightMultiplier;
    }

    public static double unusualFishWeightMultiplier() {
        return unusualFishWeightMultiplier;
    }

    private static Properties defaults() {
        Properties properties = new Properties();
        properties.setProperty("alexsmobs_weight_multiplier", "0.25");
        properties.setProperty("aquaculture_weight_multiplier", "0.25");
        properties.setProperty("unusualfishmod_weight_multiplier", "0.25");
        return properties;
    }

    private static double fraction(Properties properties, String key, double fallback) {
        try {
            double value = Double.parseDouble(properties.getProperty(key, Double.toString(fallback)).trim());
            return Math.max(0.0D, Math.min(1.0D, value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
