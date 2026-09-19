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
        return Files.isRegularFile(installMarker(packDir));
    }

    public static void ensureRoot(Path gameDir) throws IOException {
        Files.createDirectories(root(gameDir));
    }
}
