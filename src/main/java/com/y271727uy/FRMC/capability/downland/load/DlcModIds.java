package com.y271727uy.FRMC.capability.downland.load;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads {@code [[mods]].modId} from a jar's {@code META-INF/mods.toml}.
 * Jars without a readable toml yield an empty list and must be skipped.
 */
public final class DlcModIds {
    public static final String MODS_TOML = "META-INF/mods.toml";
    static final int MAX_MODS_TOML_BYTES = 256 * 1024;

    static final Set<String> RESERVED = Set.of(
            "minecraft",
            "forge",
            "fml",
            "mcp",
            "java",
            "lowcode",
            "mclanguage",
            "fmlcore",
            "frmc"
    );

    private DlcModIds() {}

    public static List<String> read(Path jar) {
        if (jar == null || !jar.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
            return List.of();
        }
        try (ZipFile zip = new ZipFile(jar.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry entry = zip.getEntry(MODS_TOML);
            if (entry == null || entry.isDirectory()) {
                return List.of();
            }
            String toml = readEntry(zip, entry);
            if (toml == null) {
                return List.of();
            }
            return parse(toml);
        } catch (IOException | RuntimeException ignored) {
            return List.of();
        }
    }

    public static List<String> parse(String toml) {
        Config parsed;
        try {
            parsed = toml == null || toml.isBlank()
                    ? Config.inMemory()
                    : new TomlParser().parse(toml);
        } catch (RuntimeException ignored) {
            return List.of();
        }

        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (Config table : modTables(parsed)) {
            String id = readString(table.get("modId"));
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    public static boolean isReserved(String modId) {
        return modId != null && RESERVED.contains(modId.toLowerCase(Locale.ROOT));
    }

    private static List<Config> modTables(Config parsed) {
        Object mods = parsed.get("mods");
        if (mods instanceof List<?> list) {
            List<Config> tables = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Config config) {
                    tables.add(config);
                }
            }
            return tables;
        }
        if (mods instanceof Config config) {
            return List.of(config);
        }
        return List.of();
    }

    private static String readString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String readEntry(ZipFile zip, ZipEntry entry) throws IOException {
        if (entry.getSize() > MAX_MODS_TOML_BYTES) {
            return null;
        }
        try (InputStream in = zip.getInputStream(entry)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int total = 0;
            int read;
            while ((read = in.read(buffer)) >= 0) {
                total += read;
                if (total > MAX_MODS_TOML_BYTES) {
                    return null;
                }
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8);
        }
    }
}
