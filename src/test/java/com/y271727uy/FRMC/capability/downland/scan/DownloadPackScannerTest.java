package com.y271727uy.FRMC.capability.downland.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadPackScannerTest {
    @TempDir
    Path tempDir;

    @Test
    void acceptsIdOnlyZipName() throws IOException {
        Path zip = writeZip("vietnam.zip", Map.of("pack.toml", dlcToml("vietnam", "1.0.0")));

        DownloadPackEntry entry = DownloadPackScanner.inspect(zip, "vietnam.zip");

        assertTrue(entry.valid());
        assertEquals("vietnam", entry.dlc().id());
        assertEquals("1.0.0", entry.dlc().version());
        assertNull(entry.issue());
    }

    @Test
    void acceptsIdDashMatchingVersion() throws IOException {
        Path zip = writeZip("vietnam-1.0.0.zip", Map.of("pack.toml", dlcToml("vietnam", "1.0.0")));

        DownloadPackEntry entry = DownloadPackScanner.inspect(zip, "vietnam-1.0.0.zip");

        assertTrue(entry.valid());
        assertEquals("vietnam", entry.dlc().id());
        assertEquals("1.0.0", entry.dlc().version());
    }

    @Test
    void acceptsMrpackWithSameRulesAsZip() throws IOException {
        Path pack = writeZip("vietnam-1.0.0.mrpack", Map.of("pack.toml", dlcToml("vietnam", "1.0.0")));

        DownloadPackEntry entry = DownloadPackScanner.inspect(pack, "vietnam-1.0.0.mrpack");

        assertTrue(entry.valid());
        assertEquals("vietnam", entry.dlc().id());
        assertEquals("1.0.0", entry.dlc().version());
        assertNull(entry.issue());
    }

    @Test
    void rejectsMrpackVersionMismatch() throws IOException {
        Path pack = writeZip("vietnam-1.0.0.mrpack", Map.of("pack.toml", dlcToml("vietnam", "1.1.0")));

        DownloadPackEntry entry = DownloadPackScanner.inspect(pack, "vietnam-1.0.0.mrpack");

        assertFalse(entry.valid());
        assertEquals(DownloadPackIssue.NAME_MISMATCH, entry.issue());
    }

    @Test
    void rejectsVersionMismatch() throws IOException {
        Path zip = writeZip("vietnam-1.0.0.zip", Map.of("pack.toml", dlcToml("vietnam", "1.1.0")));

        DownloadPackEntry entry = DownloadPackScanner.inspect(zip, "vietnam-1.0.0.zip");

        assertFalse(entry.valid());
        assertEquals(DownloadPackIssue.NAME_MISMATCH, entry.issue());
        assertEquals("vietnam", entry.dlc().id());
        assertEquals("1.1.0", entry.dlc().version());
    }

    @Test
    void rejectsCaseMismatchBetweenZipAndDlcId() throws IOException {
        Path zip = writeZip("Vietnam.zip", Map.of("pack.toml", dlcToml("vietnam", "1.0.0")));

        DownloadPackEntry entry = DownloadPackScanner.inspect(zip, "Vietnam.zip");

        assertFalse(entry.valid());
        assertEquals(DownloadPackIssue.NAME_MISMATCH, entry.issue());
    }

    @Test
    void missingRootTomlIsRejectedEvenIfNestedExists() throws IOException {
        Path missing = writeZip("vietnam.zip", Map.of("readme.txt", "no toml"));
        Path nested = writeZip("nested.zip", Map.of("foo/pack.toml", dlcToml("vietnam", "1.0.0")));

        assertEquals(DownloadPackIssue.MISSING_PACK_TOML, DownloadPackScanner.inspect(missing, "vietnam.zip").issue());
        assertEquals(DownloadPackIssue.MISSING_PACK_TOML, DownloadPackScanner.inspect(nested, "nested.zip").issue());
    }

    @Test
    void multipleDlcTablesAreRejected() throws IOException {
        Path zip = writeZip("vietnam.zip", Map.of("pack.toml", dlcToml("vietnam", "1.0.0") + dlcToml("other", "2.0.0")));

        DownloadPackEntry entry = DownloadPackScanner.inspect(zip, "vietnam.zip");

        assertFalse(entry.valid());
        assertEquals(DownloadPackIssue.MULTIPLE_DLC, entry.issue());
    }

    @Test
    void noDlcAndBrokenTomlAreRejected() throws IOException {
        Path empty = writeZip("vietnam.zip", Map.of("pack.toml", "name = \"not a dlc\"\n"));
        Path broken = writeZip("broken.zip", Map.of("pack.toml", "[[dlc]\nDLCId=\"vietnam\"\n"));

        assertEquals(DownloadPackIssue.NO_DLC, DownloadPackScanner.inspect(empty, "vietnam.zip").issue());
        assertEquals(DownloadPackIssue.PARSE_FAILED, DownloadPackScanner.inspect(broken, "broken.zip").issue());
    }

    @Test
    void scanReadsOnlyTopLevelZipsAndKeepsInvalidEntries() throws IOException {
        writeZip("vietnam.zip", Map.of("pack.toml", dlcToml("vietnam", "1.0.0")));
        writeZip("vietnam-1.0.0.zip", Map.of("pack.toml", dlcToml("vietnam", "1.1.0")));
        writeZip("vietnam.mrpack", Map.of("pack.toml", dlcToml("vietnam", "1.0.0")));
        Files.writeString(tempDir.resolve("notes.txt"), "not a zip");
        Files.write(tempDir.resolve("not-a-zip.zip"), "plain text".getBytes(StandardCharsets.UTF_8));
        Path nested = Files.createDirectory(tempDir.resolve("nested"));
        writeZip(nested.resolve("hidden.zip"), Map.of("pack.toml", dlcToml("hidden", "1.0.0")));
        writeZip(nested.resolve("hidden.mrpack"), Map.of("pack.toml", dlcToml("hidden", "1.0.0")));

        List<DownloadPackEntry> entries = DownloadPackScanner.scan(tempDir);

        assertEquals(4, entries.size());
        assertEquals("not-a-zip.zip", entries.get(0).fileName());
        assertEquals(DownloadPackIssue.UNREADABLE, entries.get(0).issue());
        assertEquals("vietnam-1.0.0.zip", entries.get(1).fileName());
        assertEquals(DownloadPackIssue.NAME_MISMATCH, entries.get(1).issue());
        assertEquals("vietnam.mrpack", entries.get(2).fileName());
        assertTrue(entries.get(2).valid());
        assertEquals("vietnam.zip", entries.get(3).fileName());
        assertTrue(entries.get(3).valid());
    }

    @Test
    void parseRejectsInvalidDlcId() {
        DownloadPackToml.ParseResult result = DownloadPackToml.parse("""
                [[dlc]]
                DLCId="foo/bar"
                version="1.0.0"
                """);

        assertFalse(result.ok());
        assertEquals(DownloadPackIssue.INVALID_DLC_ID, result.issue());
    }

    private static String dlcToml(String id, String version) {
        return """
                [[dlc]]
                DLCId="%s"
                license="MIT"
                modpack_version="1.20.1"
                version="%s"
                authors="name"
                description="test pack"
                dlc_type="content"
                """.formatted(id, version);
    }

    private Path writeZip(String fileName, Map<String, String> entries) throws IOException {
        return writeZip(tempDir.resolve(fileName), entries);
    }

    private static Path writeZip(Path path, Map<String, String> entries) throws IOException {
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
