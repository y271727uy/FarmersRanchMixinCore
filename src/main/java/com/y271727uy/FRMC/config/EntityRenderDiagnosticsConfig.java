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
                properties.setProperty("combinedRotation", "true");
                properties.setProperty("fastTraversal", "false");
                properties.setProperty("soaShadow", "false");
                properties.setProperty("batchValidation", "false");
                properties.setProperty("gpuPoseValidation", "false");
                properties.setProperty("gpuPoseRendering", "false");
                properties.setProperty("gpuGeneralModels", "false");
                properties.setProperty("gpuModelBatching", "true");
                properties.setProperty("bottomFaceProbe", "false");
                properties.setProperty("bottomFaceCulling", "false");
                properties.setProperty("partCulling", "false");
                properties.setProperty("boarLod", "false");
                properties.setProperty("generalLod", "false");
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
        Config.citadelCombinedRotationEnabled = Boolean.parseBoolean(
                properties.getProperty("combinedRotation", "true").trim()
        );
        Config.citadelFastTraversalEnabled = Boolean.parseBoolean(
                properties.getProperty("fastTraversal", "false").trim()
        );
        Config.citadelSoaShadowEnabled = Boolean.parseBoolean(
                properties.getProperty("soaShadow", "false").trim()
        );
        Config.citadelBatchValidationEnabled = Boolean.parseBoolean(
                properties.getProperty("batchValidation", "false").trim()
        );
        Config.citadelGpuPoseValidationEnabled = Boolean.parseBoolean(
                properties.getProperty("gpuPoseValidation", "false").trim()
        );
        Config.citadelGpuPoseRenderingEnabled = Boolean.parseBoolean(
                properties.getProperty("gpuPoseRendering", "false").trim()
        );
        Config.citadelGpuGeneralModelsEnabled = Boolean.parseBoolean(
                properties.getProperty("gpuGeneralModels", "false").trim()
        );
        Config.citadelGpuModelBatchingEnabled = Boolean.parseBoolean(
                properties.getProperty("gpuModelBatching", "true").trim()
        );
        Config.citadelBottomFaceProbeEnabled = Boolean.parseBoolean(
                properties.getProperty("bottomFaceProbe", "false").trim()
        );
        Config.citadelBottomFaceCullingEnabled = Boolean.parseBoolean(
                properties.getProperty("bottomFaceCulling", "false").trim()
        );
        Config.citadelPartCullingEnabled = Boolean.parseBoolean(
                properties.getProperty("partCulling", "false").trim()
        );
        Config.citadelBoarLodEnabled = Boolean.parseBoolean(
                properties.getProperty("boarLod", "false").trim()
        );
        Config.citadelGeneralLodEnabled = Boolean.parseBoolean(
                properties.getProperty("generalLod", "false").trim()
        );
    }
}
