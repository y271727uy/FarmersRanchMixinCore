package com.y271727uy.FRMC.capability.downland.install;

import com.y271727uy.FRMC.capability.downland.scan.ParsedDlc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DlcUnpackerTest {
    @TempDir
    Path tempDir;

    @Test
    void unpacksArchiveIntoDlcIdNotVersionedFolder() throws IOException {
        Path archive = writeZip("vietnam-1.0.0.zip", Map.of(
                "pack.toml", dlcToml("vietnam", "1.0.0"),
                "mods/example.jar", "jar-bytes",
                "kubejs/server_scripts/a.js", "console.log(1);\n"
        ));
        ParsedDlc dlc = new ParsedDlc("vietnam", "Vietnam", "1.0.0", "name", "desc", "MIT", "1.20.1", "content", "");

        DlcUnpacker.Result result = DlcUnpacker.unpack(archive, "vietnam-1.0.0.zip", dlc, tempDir);

        Path packDir = DlcPaths.packDir(tempDir, "vietnam");
        assertEquals(DlcUnpacker.Result.Status.SUCCESS, result.status());
        assertEquals(packDir, result.packDir());
        assertTrue(Files.isRegularFile(packDir.resolve("pack.toml")));
        assertEquals("jar-bytes", Files.readString(packDir.resolve("mods/example.jar")));
        assertTrue(Files.isRegularFile(packDir.resolve("kubejs/server_scripts/a.js")));
        assertFalse(Files.exists(DlcPaths.packDir(tempDir, "vietnam-1.0.0")));
        String marker = Files.readString(DlcPaths.installMarker(packDir));
        assertTrue(marker.contains("\"id\":\"vietnam\""));
        assertTrue(marker.contains("\"version\":\"1.0.0\""));
        assertTrue(marker.contains("\"source\":\"vietnam-1.0.0.zip\""));
    }

    @Test
    void unpacksMrpackTheSameWay() throws IOException {
        Path archive = writeZip("vietnam.mrpack", Map.of("pack.toml", dlcToml("vietnam", "1.0.0")));
        ParsedDlc dlc = new ParsedDlc("vietnam", "Vietnam", "1.0.0", "", "", "", "", "", "");

        DlcUnpacker.Result result = DlcUnpacker.unpack(archive, "vietnam.mrpack", dlc, tempDir);

        assertEquals(DlcUnpacker.Result.Status.SUCCESS, result.status());
        assertTrue(Files.isRegularFile(DlcPaths.packDir(tempDir, "vietnam").resolve("pack.toml")));
    }

    @Test
    void zipSlipIsRejectedAndDoesNotWriteFiles() throws IOException {
        Path archive = writeZip("vietnam.zip", Map.of(
                "pack.toml", dlcToml("vietnam", "1.0.0"),
                "../evil.txt", "nope"
        ));
        ParsedDlc dlc = new ParsedDlc("vietnam", "Vietnam", "1.0.0", "", "", "", "", "", "");

        DlcUnpacker.Result result = DlcUnpacker.unpack(archive, "vietnam.zip", dlc, tempDir);

        assertEquals(DlcUnpacker.Result.Status.REJECTED, result.status());
        assertFalse(Files.exists(tempDir.resolve("evil.txt")));
        assertFalse(DlcPaths.isInstalled(DlcPaths.packDir(tempDir, "vietnam")));
        assertFalse(Files.exists(DlcPaths.packDir(tempDir, "vietnam").resolve("pack.toml")));
    }

    @Test
    void alreadyInstalledIsRefused() throws IOException {
        Path packDir = DlcPaths.packDir(tempDir, "vietnam");
        Files.createDirectories(packDir);
        Files.writeString(DlcPaths.installMarker(packDir), "{\"id\":\"vietnam\"}\n");
        Path archive = writeZip("vietnam.zip", Map.of("pack.toml", dlcToml("vietnam", "1.0.0")));
        ParsedDlc dlc = new ParsedDlc("vietnam", "Vietnam", "1.0.0", "", "", "", "", "", "");

        DlcUnpacker.Result result = DlcUnpacker.unpack(archive, "vietnam.zip", dlc, tempDir);

        assertEquals(DlcUnpacker.Result.Status.ALREADY_INSTALLED, result.status());
        assertFalse(Files.exists(packDir.resolve("pack.toml")));
    }

    @Test
    void incompleteFolderCanBeOverwritten() throws IOException {
        Path packDir = DlcPaths.packDir(tempDir, "vietnam");
        Files.createDirectories(packDir);
        Files.writeString(packDir.resolve("stale.txt"), "old");
        Path archive = writeZip("vietnam.zip", Map.of("pack.toml", dlcToml("vietnam", "1.0.0")));
        ParsedDlc dlc = new ParsedDlc("vietnam", "Vietnam", "1.0.0", "", "", "", "", "", "");

        DlcUnpacker.Result result = DlcUnpacker.unpack(archive, "vietnam.zip", dlc, tempDir);

        assertEquals(DlcUnpacker.Result.Status.SUCCESS, result.status());
        assertTrue(Files.isRegularFile(packDir.resolve("pack.toml")));
        assertTrue(DlcPaths.isInstalled(packDir));
    }

    @Test
    void archiveInstallJsonIsReplacedByOurs() throws IOException {
        Path archive = writeZip("vietnam.zip", Map.of(
                "pack.toml", dlcToml("vietnam", "1.0.0"),
                "install.json", "{\"id\":\"forged\"}\n"
        ));
        ParsedDlc dlc = new ParsedDlc("vietnam", "Vietnam", "2.0.0", "", "", "", "", "", "");

        DlcUnpacker.unpack(archive, "vietnam.zip", dlc, tempDir);

        String marker = Files.readString(DlcPaths.installMarker(DlcPaths.packDir(tempDir, "vietnam")));
        assertTrue(marker.contains("\"version\":\"2.0.0\""));
        assertFalse(marker.contains("forged"));
    }

    @Test
    void resolveInsideRejectsEscapes() {
        Path packDir = DlcPaths.packDir(tempDir, "vietnam");

        assertTrue(DlcUnpacker.resolveInside(packDir, "pack.toml").endsWith("pack.toml"));
        assertTrue(DlcUnpacker.resolveInside(packDir, "mods/a.jar").endsWith(Path.of("mods", "a.jar")));
        assertEquals(null, DlcUnpacker.resolveInside(packDir, "../evil.txt"));
        assertEquals(null, DlcUnpacker.resolveInside(packDir, "/abs.txt"));
        assertEquals(null, DlcUnpacker.resolveInside(packDir, "C:/Windows/evil.txt"));
        assertEquals(null, DlcUnpacker.resolveInside(packDir, "mods/../../outside.txt"));
        assertEquals(null, DlcUnpacker.resolveInside(packDir, ""));
    }

    private static String dlcToml(String id, String version) {
        return """
                [[dlc]]
                DLCId="%s"
                version="%s"
                """.formatted(id, version);
    }

    private Path writeZip(String fileName, Map<String, String> entries) throws IOException {
        Path path = tempDir.resolve(fileName);
        Map<String, String> ordered = new LinkedHashMap<>(entries);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(path))) {
            for (Map.Entry<String, String> entry : ordered.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return path;
    }
}
