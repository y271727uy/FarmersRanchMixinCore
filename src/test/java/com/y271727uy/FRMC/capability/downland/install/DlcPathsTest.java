package com.y271727uy.FRMC.capability.downland.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DlcPathsTest {
    @TempDir
    Path gameDir;

    @Test
    void rootAndPackDirUseIdWithoutVersion() {
        assertEquals(gameDir.resolve("dlc"), DlcPaths.root(gameDir));
        assertEquals(gameDir.resolve("dlc").resolve("vietnam"), DlcPaths.packDir(gameDir, "vietnam"));
        assertFalse(DlcPaths.packDir(gameDir, "vietnam").endsWith("vietnam-1.0.0"));
    }

    @Test
    void ensureRootCreatesDlcDirectory() throws IOException {
        DlcPaths.ensureRoot(gameDir);

        assertTrue(Files.isDirectory(DlcPaths.root(gameDir)));
    }

    @Test
    void installedRequiresInstallJson() throws IOException {
        Path packDir = DlcPaths.packDir(gameDir, "vietnam");
        Files.createDirectories(packDir);

        assertFalse(DlcPaths.isInstalled(packDir));

        Files.writeString(DlcPaths.installMarker(packDir), "{\"id\":\"vietnam\"}\n");

        assertTrue(DlcPaths.isInstalled(packDir));
    }
}
