package com.y271727uy.FRMC.capability.downland.load;

import com.y271727uy.FRMC.capability.downland.install.DlcPaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DlcKubejsScanTest {
    @TempDir
    Path gameDir;

    @Test
    void scansInstalledExamplePackInRunDirectory() {
        Path runDir = Path.of("run").toAbsolutePath().normalize();
        assumeTrue(Files.isRegularFile(runDir.resolve("dlc/example/install.json")));
        assumeTrue(Files.isRegularFile(runDir.resolve("dlc/example/kubejs/startup_scripts/frmc_dlc_probe.js")));
        assumeTrue(Files.isRegularFile(runDir.resolve("kubejs/startup_scripts/example.js")));

        List<DlcKubejsScan.DlcKubejsPack> found = DlcKubejsScan.installedScripts(runDir, "startup");

        assertEquals(1, found.size());
        DlcKubejsScan.DlcKubejsPack pack = found.get(0);
        assertEquals("example", pack.packId());
        assertEquals("frmc_dlc_example", pack.namespace());
        assertEquals(
            runDir.resolve("dlc/example/kubejs/startup_scripts").toAbsolutePath().normalize(),
            pack.directory()
        );
        assertTrue(pack.files().contains("frmc_dlc_probe.js"));
        assertFalse(pack.files().contains("example.js"));
        assertFalse(pack.files().stream().anyMatch(name -> name.endsWith(".txt") || name.endsWith(".d.ts")));
        assertEquals(List.of("frmc_dlc_probe.js"), DlcKubejsScan.installedScripts(runDir, "server").get(0).files());
        assertEquals(List.of("frmc_dlc_probe.js"), DlcKubejsScan.installedScripts(runDir, "client").get(0).files());
    }

    @Test
    void rootCollisionDropsDlcScript() throws IOException {
        writeRoot("startup_scripts/main.js");
        writeInstalled("vietnam", "startup_scripts/main.js");

        assertTrue(DlcKubejsScan.installedScripts(gameDir, "startup").isEmpty());
    }

    @Test
    void uniqueDlcScriptIsAccepted() throws IOException {
        writeRoot("startup_scripts/main.js");
        Path dlcFile = writeInstalled("vietnam", "startup_scripts/extra.js");

        List<DlcKubejsScan.DlcKubejsPack> found = DlcKubejsScan.installedScripts(gameDir, "startup");

        assertEquals(1, found.size());
        DlcKubejsScan.DlcKubejsPack pack = found.get(0);
        assertEquals("vietnam", pack.packId());
        assertEquals("frmc_dlc_vietnam", pack.namespace());
        assertEquals(dlcFile.getParent().toAbsolutePath().normalize(), pack.directory());
        assertEquals(List.of("extra.js"), pack.files());
    }

    @Test
    void packWithoutInstallJsonIsIgnored() throws IOException {
        Path file = writeScript(DlcPaths.packDir(gameDir, "vietnam").resolve("kubejs/startup_scripts/extra.js"));

        assertTrue(DlcKubejsScan.installedScripts(gameDir, "startup").isEmpty());
        assertTrue(Files.isRegularFile(file));
    }

    @Test
    void uninstalledDirectoryWithFilesIsNotScanned() throws IOException {
        writeScript(DlcPaths.packDir(gameDir, "vietnam").resolve("kubejs/startup_scripts/extra.js"));
        Files.createDirectories(DlcPaths.packDir(gameDir, "vietnam"));

        assertTrue(DlcKubejsScan.installedScripts(gameDir, "startup").isEmpty());
    }

    @Test
    void dlcCollisionsKeepFirstPackThenFirstFileName() throws IOException {
        writeInstalled("alpha", "startup_scripts/shared.js");
        writeInstalled("alpha", "startup_scripts/z-last.js");
        writeInstalled("beta", "startup_scripts/shared.js");
        writeInstalled("beta", "startup_scripts/only-beta.js");

        List<DlcKubejsScan.DlcKubejsPack> found = DlcKubejsScan.installedScripts(gameDir, "startup");

        assertEquals(2, found.size());
        assertEquals("alpha", found.get(0).packId());
        assertEquals(List.of("shared.js", "z-last.js"), found.get(0).files());
        assertEquals("beta", found.get(1).packId());
        assertEquals(List.of("only-beta.js"), found.get(1).files());
    }

    @Test
    void nonJsTsFilesAreSkipped() throws IOException {
        writeInstalled("vietnam", "startup_scripts/notes.txt");
        writeInstalled("vietnam", "startup_scripts/data.json");
        writeInstalled("vietnam", "startup_scripts/ok.js");

        List<DlcKubejsScan.DlcKubejsPack> found = DlcKubejsScan.installedScripts(gameDir, "startup");

        assertEquals(List.of("ok.js"), found.get(0).files());
    }

    @Test
    void declarationFilesAreSkipped() throws IOException {
        writeInstalled("vietnam", "startup_scripts/index.d.ts");
        writeInstalled("vietnam", "startup_scripts/real.ts");

        List<DlcKubejsScan.DlcKubejsPack> found = DlcKubejsScan.installedScripts(gameDir, "startup");

        assertEquals(List.of("real.ts"), found.get(0).files());
    }

    @Test
    void typescriptFilesAreAccepted() throws IOException {
        writeInstalled("vietnam", "startup_scripts/extra.ts");

        assertEquals(List.of("extra.ts"), DlcKubejsScan.installedScripts(gameDir, "startup").get(0).files());
    }

    @Test
    void scriptExtensionIsCaseInsensitive() throws IOException {
        writeInstalled("vietnam", "startup_scripts/Extra.JS");

        assertEquals(List.of("Extra.JS"), DlcKubejsScan.installedScripts(gameDir, "startup").get(0).files());
    }

    @Test
    void emptyPackIsNotListed() throws IOException {
        writeInstalled("vietnam", "startup_scripts/notes.txt");

        assertTrue(DlcKubejsScan.installedScripts(gameDir, "startup").isEmpty());
    }

    @Test
    void nestedScriptsWithinDepthAreAccepted() throws IOException {
        writeInstalled("vietnam", "startup_scripts/nested/more/extra.js");

        assertEquals(List.of("nested/more/extra.js"), DlcKubejsScan.installedScripts(gameDir, "startup").get(0).files());
    }

    @Test
    void pathCollisionIsCaseInsensitive() throws IOException {
        writeRoot("startup_scripts/Main.js");
        writeInstalled("vietnam", "startup_scripts/main.js");

        assertTrue(DlcKubejsScan.installedScripts(gameDir, "startup").isEmpty());
    }

    @Test
    void scriptTypesDoNotShareFolders() throws IOException {
        writeInstalled("vietnam", "startup_scripts/extra.js");
        writeInstalled("vietnam", "server_scripts/server.js");

        assertEquals(List.of("extra.js"), DlcKubejsScan.installedScripts(gameDir, "startup").get(0).files());
        assertEquals(List.of("server.js"), DlcKubejsScan.installedScripts(gameDir, "server").get(0).files());
        assertTrue(DlcKubejsScan.installedScripts(gameDir, "client").isEmpty());
    }

    private Path writeInstalled(String packId, String relative) throws IOException {
        Path packDir = DlcPaths.packDir(gameDir, packId);
        Files.createDirectories(packDir);
        Files.writeString(DlcPaths.installMarker(packDir), "{\"id\":\"" + packId + "\"}\n");
        return writeScript(packDir.resolve("kubejs").resolve(relative));
    }

    private Path writeRoot(String relative) throws IOException {
        return writeScript(gameDir.resolve("kubejs").resolve(relative));
    }

    private static Path writeScript(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, "// " + path.getFileName() + "\n");
        return path;
    }
}
