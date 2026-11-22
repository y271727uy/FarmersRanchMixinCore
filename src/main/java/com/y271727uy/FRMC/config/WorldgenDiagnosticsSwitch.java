package com.y271727uy.FRMC.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 世界生成诊断的加载开关。它在 mixin 加载阶段就要被读取（此时 Forge 配置还没加载），
 * 所以用一个简单的 properties 文件，或 JVM 参数 {@code -Dfrmc.biomeLookupDiagnostics=true}。
 *
 * <p>关闭时诊断 mixin 根本不会被织入，对生产环境没有任何开销。本类不能引用 Minecraft 类。
 */
public final class WorldgenDiagnosticsSwitch {
    private static final String FILE_NAME = "frmc-worldgen-diagnostics.properties";
    private static final String BIOME_LOOKUP_KEY = "biomeLookupDiagnostics";
    private static final String DEFAULT_CONTENT = """
        # FRMC world generation diagnostics. Read once at game start, before mods load; restart to apply.
        # These only measure and log. They never change world generation.
        #
        # biomeLookupDiagnostics: every 10 s, log how often MultiNoiseBiomeSource.getNoiseBiome is asked for the
        # same biome cell again, what simulated caches of a few sizes would hit, the sampled cost of one lookup,
        # and which callers repeat. Used to decide whether a biome cell cache is worth building.
        # Also enabled by the JVM argument -Dfrmc.biomeLookupDiagnostics=true
        biomeLookupDiagnostics=false
        """;

    private static volatile Boolean biomeLookup;

    private WorldgenDiagnosticsSwitch() {
    }

    public static boolean biomeLookupDiagnostics() {
        Boolean cached = biomeLookup;
        if (cached == null) {
            synchronized (WorldgenDiagnosticsSwitch.class) {
                cached = biomeLookup;
                if (cached == null) {
                    cached = Boolean.getBoolean("frmc." + BIOME_LOOKUP_KEY) || readFlag(BIOME_LOOKUP_KEY);
                    biomeLookup = cached;
                }
            }
        }
        return cached;
    }

    private static boolean readFlag(String key) {
        try {
            Path path = FRMCConfigPaths.resolve(FILE_NAME);
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, DEFAULT_CONTENT, StandardCharsets.UTF_8);
                return false;
            }
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            return Boolean.parseBoolean(properties.getProperty(key, "false").trim());
        } catch (IOException | RuntimeException e) {
            System.err.println("[FRMC] Could not read " + FILE_NAME + ", world generation diagnostics stay off: " + e);
            return false;
        }
    }
}
