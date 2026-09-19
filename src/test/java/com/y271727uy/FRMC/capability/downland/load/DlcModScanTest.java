package com.y271727uy.FRMC.capability.downland.load;

import com.y271727uy.FRMC.capability.downland.install.DlcPaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DlcModScanTest {
    @TempDir
    Path gameDir;

    @Test
    void rootCollisionDropsDlcJar() throws IOException {
        writeModJar(gameDir.resolve("mods"), "farmersdelight.jar", "farmersdelight");
        Path dlcJar = writeInstalledDlcJar("vietnam", "farmersdelight-extra.jar", "farmersdelight");

        List<Path> found = DlcModScan.installedModJars(gameDir);

        assertFalse(found.contains(dlcJar.toAbsolutePath().normalize()));
        assertTrue(found.isEmpty());
    }

    @Test
    void uniqueDlcJarIsAccepted() throws IOException {
        writeModJar(gameDir.resolve("mods"), "farmersdelight.jar", "farmersdelight");
        Path dlcJar = writeInstalledDlcJar("vietnam", "vietnamfood.jar", "vietnamfood");

        List<Path> found = DlcModScan.installedModJars(gameDir);

        assertEquals(List.of(dlcJar.toAbsolutePath().normalize()), found);
    }

    @Test
    void packWithoutInstallJsonIsIgnored() throws IOException {
        Path jar = writeModJar(DlcPaths.packDir(gameDir, "vietnam").resolve("mods"), "vietnamfood.jar", "vietnamfood");

        List<Path> found = DlcModScan.installedModJars(gameDir);

        assertTrue(found.isEmpty());
        assertFalse(DlcPaths.isInstalled(DlcPaths.packDir(gameDir, "vietnam")));
        assertTrue(Files.isRegularFile(jar));
    }

    @Test
    void dlcCollisionsKeepFirstPackThenFirstFileName() throws IOException {
        Path alpha = writeInstalledDlcJar("alpha", "z-last.jar", "shared");
        writeInstalledDlcJar("alpha", "a-first.jar", "shared");
        writeInstalledDlcJar("beta", "shared.jar", "shared");

        List<Path> found = DlcModScan.installedModJars(gameDir);

        assertEquals(List.of(alpha.getParent().resolve("a-first.jar").toAbsolutePath().normalize()), found);
    }

    @Test
    void reservedModIdIsDroppedEvenWithoutRootJar() throws IOException {
        writeInstalledDlcJar("vietnam", "fake-forge.jar", "Forge");

        assertTrue(DlcModScan.installedModJars(gameDir).isEmpty());
    }

    @Test
    void jarWithoutModsTomlIsSkipped() throws IOException {
        Path packMods = DlcPaths.packDir(gameDir, "vietnam").resolve("mods");
        Files.createDirectories(packMods);
        Files.writeString(DlcPaths.installMarker(DlcPaths.packDir(gameDir, "vietnam")), "{\"id\":\"vietnam\"}\n");
        writeZip(packMods.resolve("library.jar"), Map.of("readme.txt", "no toml"));

        assertTrue(DlcModScan.installedModJars(gameDir).isEmpty());
    }

    @Test
    void oneCollidingModIdDropsTheWholeJar() throws IOException {
        writeModJar(gameDir.resolve("mods"), "root.jar", "rootmod");
        writeInstalledDlcJarToml("vietnam", "bundle.jar", modsToml("rootmod", "vietnamfood"));

        assertTrue(DlcModScan.installedModJars(gameDir).isEmpty());
    }

    @Test
    void droppedBundleDoesNotClaimItsOtherIds() throws IOException {
        writeModJar(gameDir.resolve("mods"), "root.jar", "rootmod");
        writeInstalledDlcJarToml("alpha", "bundle.jar", modsToml("rootmod", "vietnamfood"));
        Path later = writeInstalledDlcJar("beta", "vietnamfood.jar", "vietnamfood");

        assertEquals(List.of(later.toAbsolutePath().normalize()), DlcModScan.installedModJars(gameDir));
    }

    @Test
    void nestedJarsAreNotScanned() throws IOException {
        writeModJar(gameDir.resolve("mods").resolve("nested"), "hidden-root.jar", "hidden");
        Path accepted = writeInstalledDlcJar("vietnam", "vietnamfood.jar", "vietnamfood");
        writeModJar(DlcPaths.packDir(gameDir, "vietnam").resolve("mods").resolve("nested"), "hidden-dlc.jar", "hidden");

        List<Path> found = DlcModScan.installedModJars(gameDir);

        assertEquals(List.of(accepted.toAbsolutePath().normalize()), found);
    }

    @Test
    void jarExtensionIsCaseInsensitive() throws IOException {
        Path dlcJar = writeInstalledDlcJar("vietnam", "VietnamFood.JAR", "vietnamfood");

        List<Path> found = DlcModScan.installedModJars(gameDir);

        assertEquals(List.of(dlcJar.toAbsolutePath().normalize()), found);
    }

    @Test
    void ownJarIsIgnoredOnExplodedClasspath() {
        assertNull(DlcModSelfJar.locate(DlcModScan.class));
    }

    @Test
    void jarResourceUrlResolvesHostJar() throws Exception {
        Path jar = gameDir.resolve("frmc.jar");
        writeZip(jar, Map.of("marker.txt", "ok"));
        URL resource = new URL("jar:" + jar.toUri().toURL() + "!/com/example/Marker.class");

        assertEquals(jar.toAbsolutePath().normalize(), DlcModSelfJar.jarPath(resource));
    }

    private Path writeInstalledDlcJar(String packId, String fileName, String... modIds) throws IOException {
        return writeInstalledDlcJarToml(packId, fileName, modsToml(modIds));
    }

    private Path writeInstalledDlcJarToml(String packId, String fileName, String toml) throws IOException {
        Path packDir = DlcPaths.packDir(gameDir, packId);
        Files.createDirectories(packDir);
        Files.writeString(DlcPaths.installMarker(packDir), "{\"id\":\"" + packId + "\"}\n");
        return writeModJarToml(packDir.resolve("mods"), fileName, toml);
    }

    private static Path writeModJar(Path folder, String fileName, String... modIds) throws IOException {
        return writeModJarToml(folder, fileName, modsToml(modIds));
    }

    private static Path writeModJarToml(Path folder, String fileName, String toml) throws IOException {
        Files.createDirectories(folder);
        return writeZip(folder.resolve(fileName), Map.of("META-INF/mods.toml", toml));
    }

    private static String modsToml(String... modIds) {
        StringBuilder toml = new StringBuilder("modLoader=\"javafml\"\n");
        for (String modId : modIds) {
            toml.append("[[mods]]\nmodId=\"").append(modId).append("\"\n");
        }
        return toml.toString();
    }

    private static Path writeZip(Path path, Map<String, String> entries) throws IOException {
        Files.createDirectories(path.getParent());
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(path))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return path;
    }
}
