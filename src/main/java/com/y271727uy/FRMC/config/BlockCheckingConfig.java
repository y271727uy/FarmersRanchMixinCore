package com.y271727uy.FRMC.config;

import com.y271727uy.FRMC.capability.blockchecking.aggregate.BlockCheckingLimits;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** File-backed limits for server-only, read-only block checking. */
public final class BlockCheckingConfig {
    private static final Path CONFIG_PATH = FRMCConfigPaths.resolve("frmc-blockchecking.properties");
    private static volatile boolean enabled = true;
    private static volatile int defaultSessionSeconds = 30;
    private static volatile int maximumSessionSeconds = 300;
    private static volatile int sampleIntervalMillis = 25;
    private static volatile int chunkSnapshotPeriodTicks = 20;
    private static volatile int outputEntryLimit = 32;

    private BlockCheckingConfig() {
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
                    properties.store(output, "FRMC server block-checking settings");
                }
            }
        } catch (IOException ignored) {
            // Keep defaults when the optional block-checking config cannot be used.
        }
        enabled = bool(properties, "enable", true);
        defaultSessionSeconds = bounded(properties, "default_session_seconds", 30,
                BlockCheckingLimits.MIN_SESSION_SECONDS, BlockCheckingLimits.MAX_SESSION_SECONDS);
        maximumSessionSeconds = bounded(properties, "maximum_session_seconds", 300,
                defaultSessionSeconds, BlockCheckingLimits.MAX_SESSION_SECONDS);
        sampleIntervalMillis = bounded(properties, "sample_interval_millis", 25,
                BlockCheckingLimits.MIN_SAMPLE_INTERVAL_MILLIS, BlockCheckingLimits.MAX_SAMPLE_INTERVAL_MILLIS);
        chunkSnapshotPeriodTicks = bounded(properties, "chunk_snapshot_period_ticks", 100,
                BlockCheckingLimits.MIN_CHUNK_SNAPSHOT_PERIOD_TICKS, BlockCheckingLimits.MAX_CHUNK_SNAPSHOT_PERIOD_TICKS);
        outputEntryLimit = bounded(properties, "output_entry_limit", 16, 1, BlockCheckingLimits.MAX_OUTPUT_ENTRIES);
    }

    public static boolean enabled() { return enabled; }
    public static int defaultSessionSeconds() { return defaultSessionSeconds; }
    public static int maximumSessionSeconds() { return maximumSessionSeconds; }
    public static int sampleIntervalMillis() { return sampleIntervalMillis; }
    public static int chunkSnapshotPeriodTicks() { return chunkSnapshotPeriodTicks; }
    public static int outputEntryLimit() { return outputEntryLimit; }

    private static Properties defaults() {
        Properties properties = new Properties();
        properties.setProperty("enable", "true");
        properties.setProperty("default_session_seconds", "30");
        properties.setProperty("maximum_session_seconds", "300");
        properties.setProperty("sample_interval_millis", "25");
        properties.setProperty("chunk_snapshot_period_ticks", "20");
        properties.setProperty("output_entry_limit", "32");
        return properties;
    }

    private static boolean bool(Properties properties, String key, boolean fallback) {
        return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(fallback)).trim());
    }

    private static int bounded(Properties properties, String key, int fallback, int minimum, int maximum) {
        try {
            return BlockCheckingLimits.bounded(Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim()), minimum, maximum);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
