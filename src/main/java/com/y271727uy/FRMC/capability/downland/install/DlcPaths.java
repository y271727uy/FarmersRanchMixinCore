package com.y271727uy.FRMC.capability.downland.install;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Game-root layout for installed DLC: {@code <gamedir>/dlc/<id>/...}.
 * {@code id} never includes the pack version.
 */
public final class DlcPaths {
    public static final String DIRECTORY_NAME = "dlc";
    public static final String INSTALL_MARKER = "install.json";
    public static final String DISABLED_MARKER = INSTALL_MARKER + ".disabled";

    private DlcPaths() {}

    public static Path root(Path gameDir) {
        return gameDir.resolve(DIRECTORY_NAME);
    }

    public static Path packDir(Path gameDir, String id) {
        return root(gameDir).resolve(id);
    }

    public static Path installMarker(Path packDir) {
        return packDir.resolve(INSTALL_MARKER);
    }

    public static boolean isInstalled(Path packDir) {
        return Files.isRegularFile(installMarker(packDir)) && !isDisabled(packDir);
    }

    public static boolean isDisabled(Path packDir) {
        return Files.isRegularFile(packDir.resolve(DISABLED_MARKER));
    }

    public static void setEnabled(Path packDir, boolean enabled) throws IOException {
        Path install = installMarker(packDir);
        Path disabled = packDir.resolve(DISABLED_MARKER);
        Path source = enabled ? disabled : install;
        if (!Files.isRegularFile(source)) {
            throw new IOException("Missing DLC marker: " + source);
        }
        Files.move(source, enabled ? install : disabled);
    }

    public static boolean isMarkerPath(Path packDir, Path destination) {
        Path relative = packDir.toAbsolutePath().normalize().relativize(destination);
        String name = relative.getName(0).toString();
        return INSTALL_MARKER.equalsIgnoreCase(name) || DISABLED_MARKER.equalsIgnoreCase(name);
    }

    public static void ensureRoot(Path gameDir) throws IOException {
        Files.createDirectories(root(gameDir));
    }
}
