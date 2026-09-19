package com.y271727uy.FRMC.capability.downland.load;

import com.y271727uy.FRMC.capability.downland.install.DlcPaths;
import com.y271727uy.FRMC.capability.downland.scan.DownloadPackRules;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Extra Forge mod jars live under {@code dlc/<id>/mods/*.jar} of installed packs.
 * Root {@code mods/} is authoritative: any DLC jar that shares a modId with a
 * root jar, a reserved id, or an earlier DLC jar is dropped. Same jar, multiple
 * modIds — one collision drops the whole jar.
 */
public final class DlcModScan {
    public static final String MODS_FOLDER = "mods";

    private DlcModScan() {}

    public static List<Path> installedModJars(Path gameDir) {
        if (gameDir == null) {
            return List.of();
        }

        Set<String> claimed = new LinkedHashSet<>(DlcModIds.RESERVED);
        for (Path rootJar : listJars(gameDir.resolve(MODS_FOLDER))) {
            claimed.addAll(normalized(DlcModIds.read(rootJar)));
        }

        List<Path> accepted = new ArrayList<>();
        for (Path packDir : installedPacks(gameDir)) {
            for (Path jar : listJars(packDir.resolve(MODS_FOLDER))) {
                List<String> ids = DlcModIds.read(jar);
                if (ids.isEmpty()) {
                    continue;
                }
                List<String> normalized = normalized(ids);
                String collision = firstClaimed(normalized, claimed);
                if (collision != null) {
                    continue;
                }
                claimed.addAll(normalized);
                accepted.add(jar.toAbsolutePath().normalize());
            }
        }
        return accepted;
    }

    static List<Path> installedPacks(Path gameDir) {
        Path root = DlcPaths.root(gameDir);
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<Path> packs = new ArrayList<>();
        try (DirectoryStream<Path> children = Files.newDirectoryStream(root)) {
            for (Path child : children) {
                if (!Files.isDirectory(child)) {
                    continue;
                }
                String id = child.getFileName().toString();
                if (!DownloadPackRules.isValidPackId(id) || !DlcPaths.isInstalled(child)) {
                    continue;
                }
                packs.add(child);
            }
        } catch (IOException ignored) {
            return List.of();
        }
        packs.sort(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
        return packs;
    }

    static List<Path> listJars(Path folder) {
        if (folder == null || !Files.isDirectory(folder)) {
            return List.of();
        }
        List<Path> jars = new ArrayList<>();
        try (DirectoryStream<Path> children = Files.newDirectoryStream(folder)) {
            for (Path child : children) {
                if (!Files.isRegularFile(child)) {
                    continue;
                }
                String name = child.getFileName().toString();
                if (!name.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                    continue;
                }
                try {
                    if (Files.size(child) == 0) {
                        continue;
                    }
                } catch (IOException ignored) {
                    continue;
                }
                jars.add(child);
            }
        } catch (IOException ignored) {
            return List.of();
        }
        jars.sort(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
        return jars;
    }

    private static List<String> normalized(List<String> ids) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                unique.add(id.toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(unique);
    }

    private static String firstClaimed(List<String> ids, Set<String> claimed) {
        for (String id : ids) {
            if (claimed.contains(id)) {
                return id;
            }
        }
        return null;
    }
}
