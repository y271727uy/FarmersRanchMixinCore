package com.y271727uy.FRMC.capability.downland.load;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Extra KubeJS scripts live under {@code dlc/<id>/kubejs/<type>_scripts/} of
 * installed packs. Root {@code kubejs/} is authoritative: a DLC file whose
 * relative path collides with a root script or an earlier DLC script is dropped.
 */
public final class DlcKubejsScan {
    public static final String KUBEJS_FOLDER = "kubejs";
    public static final String NAMESPACE_PREFIX = "frmc_dlc_";
    static final int MAX_WALK_DEPTH = 10;

    private DlcKubejsScan() {}

    public static String scriptFolder(String typeName) {
        return typeName + "_scripts";
    }

    public static List<DlcKubejsPack> installedScripts(Path gameDir, String typeName) {
        if (gameDir == null || typeName == null || typeName.isBlank()) {
            return List.of();
        }

        String folder = scriptFolder(typeName);
        Set<String> claimed = claimedRoot(gameDir, typeName);

        List<DlcKubejsPack> packs = new ArrayList<>();
        for (Path packDir : DlcModScan.installedPacks(gameDir)) {
            Path scriptsDir = packDir.resolve(KUBEJS_FOLDER).resolve(folder);
            List<String> files = listScripts(scriptsDir, claimed);
            if (files.isEmpty()) {
                continue;
            }
            String packId = packDir.getFileName().toString();
            packs.add(new DlcKubejsPack(
                packId,
                NAMESPACE_PREFIX + packId.toLowerCase(Locale.ROOT),
                scriptsDir.toAbsolutePath().normalize(),
                files
            ));
        }
        return List.copyOf(packs);
    }

    static Set<String> claimedRoot(Path gameDir, String typeName) {
        return claimedRootFolder(gameDir.resolve(KUBEJS_FOLDER).resolve(scriptFolder(typeName)));
    }

    private static Set<String> claimedRootFolder(Path folder) {
        Set<String> claimed = new LinkedHashSet<>();
        for (Path file : walkScripts(folder)) {
            String relative = toRelative(folder, file);
            if (relative.isEmpty()) {
                continue;
            }
            claimed.add(relative.toLowerCase(Locale.ROOT));
        }
        return claimed;
    }

    private static List<String> listScripts(Path folder, Set<String> claimed) {
        List<Path> files = walkScripts(folder);
        files.sort(Comparator
            .comparing((Path path) -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(path -> toRelative(folder, path), String.CASE_INSENSITIVE_ORDER));

        List<String> accepted = new ArrayList<>();
        for (Path file : files) {
            String relative = toRelative(folder, file);
            if (relative.isEmpty()) {
                continue;
            }
            String key = relative.toLowerCase(Locale.ROOT);
            if (claimed.contains(key)) {
                continue;
            }
            claimed.add(key);
            accepted.add(relative);
        }
        return List.copyOf(accepted);
    }

    private static List<Path> walkScripts(Path folder) {
        List<Path> files = new ArrayList<>();
        if (folder == null || !Files.isDirectory(folder)) {
            return files;
        }
        try (Stream<Path> stream = Files.walk(folder, MAX_WALK_DEPTH, FileVisitOption.FOLLOW_LINKS)) {
            stream.forEach(path -> {
                if (!Files.isRegularFile(path)) {
                    return;
                }
                if (!isScriptFileName(path.getFileName().toString())) {
                    return;
                }
                files.add(path);
            });
        } catch (IOException ignored) {
            return files;
        }
        return files;
    }

    private static boolean isScriptFileName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".d.ts")) {
            return false;
        }
        return lower.endsWith(".js") || lower.endsWith(".ts");
    }

    private static String toRelative(Path root, Path file) {
        Path relative = root.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize());
        if (relative.isAbsolute() || relative.getNameCount() == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < relative.getNameCount(); i++) {
            String name = relative.getName(i).toString();
            if (".".equals(name) || "..".equals(name)) {
                return "";
            }
            if (i > 0) {
                builder.append('/');
            }
            builder.append(name);
        }
        return builder.toString();
    }

    public record DlcKubejsPack(String packId, String namespace, Path directory, List<String> files) {
        public DlcKubejsPack {
            files = List.copyOf(files);
        }
    }
}
