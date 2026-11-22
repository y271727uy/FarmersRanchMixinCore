package com.y271727uy.FRMC.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** File-backed switches for vanilla villager optimizations. Each one can be turned off on its own. */
public final class VillagerOptimizationConfig {
    private static final Path CONFIG_PATH = FRMCConfigPaths.resolve("frmc-villager.properties");
    private static final String ZERO_PROGRESS_PATH_COOLDOWN = "zero_progress_path_cooldown";
    private static final String VILLAGE_FREEZE = "village_freeze";
    private static final String VILLAGE_FREEZE_MSPT = "village_freeze_mspt";
    private static final String VILLAGE_FREEZE_RELEASE_MSPT = "village_freeze_release_mspt";
    private static final String VILLAGE_FREEZE_HOLD_TICKS = "village_freeze_hold_ticks";
    private static final String VILLAGE_FREEZE_MIN_AWAKE_TICKS = "village_freeze_min_awake_ticks";
    private static final String VILLAGE_FREEZE_RESTOCK_CATCH_UP = "village_freeze_restock_catch_up";
    private static volatile int villageFreezeMinAwakeTicks = 200;
    private static volatile boolean villageFreezeRestockCatchUp = true;
    private static volatile boolean zeroProgressPathCooldown = true;
    private static volatile boolean villageFreeze = true;
    private static volatile float villageFreezeMspt = 45.0F;
    private static volatile float villageFreezeReleaseMspt = 35.0F;
    private static volatile int villageFreezeHoldTicks = 200;

    private VillagerOptimizationConfig() {
    }

    public static void load() {
        Properties properties = defaults();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.exists(CONFIG_PATH)) {
                try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
                    properties.load(input);
                }
            }
            // Rewrite so keys added in newer versions show up in existing files.
            try (OutputStream output = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(output, """
                        FRMC vanilla villager optimizations (restart to apply).
                        zero_progress_path_cooldown: when MoveToTargetSink finds a partial path that ends in the
                          villager's own block column, skip starting it and use vanilla's 0-39 tick retry cooldown
                          instead of searching again next tick. Villagers look the same; repeated failed searches stop.
                        village_freeze: vanilla villagers leave the generic hidden-mob AI sleep and use their own rule:
                          full AI unless the server is under pressure; under pressure, villagers inside a village that
                          no player can see are frozen (never while trading, hurt, panicking, in a raid or near
                          monsters) and wake as soon as they are seen. false = villagers use the generic rules again.
                        village_freeze_mspt / village_freeze_release_mspt: average server tick time in milliseconds
                          at which pressure turns on / off. village_freeze_hold_ticks: minimum time pressure stays on.
                        village_freeze_min_awake_ticks: after waking, a villager stays awake at least this long.
                        village_freeze_restock_catch_up: a villager frozen through work hours gets, on waking, the one
                          vanilla restock check it missed at its workstation (vanilla daily limits still apply).""");
            }
        } catch (IOException ignored) {
            // Keep defaults when the optional villager config cannot be used.
        }
        zeroProgressPathCooldown = bool(properties, ZERO_PROGRESS_PATH_COOLDOWN, true);
        villageFreeze = bool(properties, VILLAGE_FREEZE, true);
        villageFreezeMspt = bounded(properties, VILLAGE_FREEZE_MSPT, 45.0F, 1.0F, 1000.0F);
        villageFreezeReleaseMspt = bounded(properties, VILLAGE_FREEZE_RELEASE_MSPT, 35.0F, 0.0F, villageFreezeMspt);
        villageFreezeHoldTicks = (int) bounded(properties, VILLAGE_FREEZE_HOLD_TICKS, 200.0F, 0.0F, 12000.0F);
        villageFreezeMinAwakeTicks = (int) bounded(properties, VILLAGE_FREEZE_MIN_AWAKE_TICKS, 200.0F, 0.0F, 12000.0F);
        villageFreezeRestockCatchUp = bool(properties, VILLAGE_FREEZE_RESTOCK_CATCH_UP, true);
    }

    public static int villageFreezeMinAwakeTicks() {
        return villageFreezeMinAwakeTicks;
    }

    public static boolean villageFreezeRestockCatchUp() {
        return villageFreezeRestockCatchUp;
    }

    public static boolean zeroProgressPathCooldown() {
        return zeroProgressPathCooldown;
    }

    public static boolean villageFreeze() {
        return villageFreeze;
    }

    public static float villageFreezeMspt() {
        return villageFreezeMspt;
    }

    public static float villageFreezeReleaseMspt() {
        return villageFreezeReleaseMspt;
    }

    public static int villageFreezeHoldTicks() {
        return villageFreezeHoldTicks;
    }

    private static Properties defaults() {
        Properties properties = new Properties();
        properties.setProperty(ZERO_PROGRESS_PATH_COOLDOWN, "true");
        properties.setProperty(VILLAGE_FREEZE, "true");
        properties.setProperty(VILLAGE_FREEZE_MSPT, "45");
        properties.setProperty(VILLAGE_FREEZE_RELEASE_MSPT, "35");
        properties.setProperty(VILLAGE_FREEZE_HOLD_TICKS, "200");
        properties.setProperty(VILLAGE_FREEZE_MIN_AWAKE_TICKS, "200");
        properties.setProperty(VILLAGE_FREEZE_RESTOCK_CATCH_UP, "true");
        return properties;
    }

    private static boolean bool(Properties properties, String key, boolean fallback) {
        return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(fallback)).trim());
    }

    private static float bounded(Properties properties, String key, float fallback, float minimum, float maximum) {
        try {
            float value = Float.parseFloat(properties.getProperty(key, Float.toString(fallback)).trim());
            return Float.isNaN(value) ? fallback : Math.max(minimum, Math.min(maximum, value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
