package com.y271727uy.FRMC.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class EntityRenderDiagnosticsConfig {
    private EntityRenderDiagnosticsConfig() {
    }

    public static void load() {
        Properties properties = new Properties();
        Path path = FRMCConfigPaths.resolve("frmc-entity-render-diagnostics.properties");
        try {
            Files.createDirectories(path.getParent());
            if (Files.exists(path)) {
                try (InputStream input = Files.newInputStream(path)) {
                    properties.load(input);
                }
            } else {
                properties.setProperty("enable", "false");
                try (OutputStream output = Files.newOutputStream(path)) {
                    properties.store(output, "FRMC UW/Citadel/AR render diagnostics");
                }
            }
        } catch (IOException ignored) {
            properties.clear();
        }
        Config.entityRenderDiagnosticsEnabled = Boolean.parseBoolean(
                properties.getProperty("enable", "false").trim()
        );
    }
}
