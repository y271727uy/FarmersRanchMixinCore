package com.y271727uy.FRMC.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/** Configuration for the experimental Chloride-assisted Mob AI sleep mode. */
public final class EntityActivityConfig {
    private static final Path CONFIG_PATH = FRMCConfigPaths.resolve("frmc-entity-activity.properties");

    private static volatile boolean enabled = true;
    private static volatile int protectionChunks = 2;
    private static volatile int evaluationIntervalTicks = 10;
    private static volatile int protectedChunkPressureThreshold = 20;
    private static volatile int hiddenGraceTicks = 40;
    private static volatile boolean overloadCleanupEnabled = true;
    private static volatile int overloadCleanupPressureThreshold = 64;
    private static volatile int overloadCleanupCooldownTicks = 6000;

    private EntityActivityConfig() {
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
                    properties.store(output, "FRMC experimental entity activity settings");
                }
            }
        } catch (IOException ignored) {
            // Keep the in-memory defaults when the config cannot be read or created.
        }
        enabled = bool(properties, "enable", true);
        protectionChunks = boundedInt(properties, "protection_chunks", 2, 0, 8);
        evaluationIntervalTicks = boundedInt(properties, "evaluation_interval_ticks", 10, 1, 200);
        protectedChunkPressureThreshold = boundedInt(properties, "protected_chunk_pressure_threshold", 20, 1, 512);
        hiddenGraceTicks = boundedInt(properties, "hidden_grace_ticks", 40, 0, 1200);
        overloadCleanupEnabled = bool(properties, "overload_cleanup_enabled", true);
        overloadCleanupPressureThreshold = boundedInt(properties, "overload_cleanup_pressure_threshold", 64, 1, 1024);
        overloadCleanupCooldownTicks = boundedInt(properties, "overload_cleanup_cooldown_ticks", 6000, 20, 72000);
    }

    public static boolean enabled() {
        return enabled;
    }

    public static int protectionChunks() {
        return protectionChunks;
    }

    public static int evaluationIntervalTicks() {
        return evaluationIntervalTicks;
    }

    /** Mob count in a protected chunk at which hidden Mob AI may run at 80% frequency. */
    public static int protectedChunkPressureThreshold() {
        return protectedChunkPressureThreshold;
    }

    /** Continuous hidden time required before a Mob may be throttled or put to sleep. */
    public static int hiddenGraceTicks() {
        return hiddenGraceTicks;
    }

    public static boolean overloadCleanupEnabled() {
        return overloadCleanupEnabled;
    }

    public static int overloadCleanupPressureThreshold() {
        return overloadCleanupPressureThreshold;
    }

    public static int overloadCleanupCooldownTicks() {
        return overloadCleanupCooldownTicks;
    }

    private static Properties defaults() {
        Properties properties = new Properties();
        properties.setProperty("enable", "true");
        properties.setProperty("protection_chunks", "2");
        properties.setProperty("evaluation_interval_ticks", "10");
        properties.setProperty("protected_chunk_pressure_threshold", "20");
        properties.setProperty("hidden_grace_ticks", "40");
        properties.setProperty("overload_cleanup_enabled", "true");
        properties.setProperty("overload_cleanup_pressure_threshold", "64");
        properties.setProperty("overload_cleanup_cooldown_ticks", "6000");
        return properties;
    }

    private static boolean bool(Properties properties, String key, boolean fallback) {
        return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(fallback)).trim());
    }

    private static int boundedInt(Properties properties, String key, int fallback, int minimum, int maximum) {
        try {
            int value = Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
            return Math.max(minimum, Math.min(maximum, value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
