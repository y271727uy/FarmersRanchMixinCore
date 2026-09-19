package com.y271727uy.FRMC.capability.downland.index;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DlcIndexParserTest {
    private static final String SHA512 = "eb932308aceb30f5157e967f1bd4b57eb1a1b2093886cbfaef6524b780337b8b"
            + "0e2bd41c5103694c3a275827ceb7d63e7c55637a7a46f8e21f0a6770346b757d";
    private static final String SHA1 = "11ad483d1fd1f7ebd84b4940b1dc0926b3e9daa8";

    @TempDir
    Path tempDir;

    @Test
    void parsesOfficialCdnFileAndDropsGithub() {
        DlcIndexFile parsed = DlcIndexParser.parse("""
                {
                  "files": [
                    {
                      "path": "mods/AI-Improvements-1.20-0.5.2.jar",
                      "hashes": {
                        "sha1": "%s",
                        "sha512": "%s"
                      },
                      "downloads": [
                        "https://mediafilez.forgecdn.net/files/4578/262/AI-Improvements-1.20-0.5.2.jar",
                        "https://edge.forgecdn.net/files/4578/262/AI-Improvements-1.20-0.5.2.jar",
                        "https://cdn.modrinth.com/data/DSVgwcji/versions/eJihmpNQ/AI-Improvements-1.20-0.5.2.jar",
                        "https://github.com/someone/repo/releases/download/v1/AI-Improvements-1.20-0.5.2.jar"
                      ],
                      "fileSize": 29553
                    }
                  ]
                }
                """.formatted(SHA1, SHA512));

        assertFalse(parsed.failed());
        assertEquals(1, parsed.files().size());
        assertEquals(0, parsed.skipped());
        DlcRemoteFile file = parsed.files().get(0);
        assertEquals("mods/AI-Improvements-1.20-0.5.2.jar", file.path());
        assertEquals(3, file.downloads().size());
        assertEquals(SHA512, file.sha512());
        assertEquals(SHA1, file.sha1());
        assertEquals(29553L, file.fileSize());
        assertFalse(file.downloads().stream().anyMatch(url -> url.contains("github.com")));
    }

    @Test
    void rejectsBadPathMissingHashAndUnwhitelistedOnly() {
        DlcIndexFile parsed = DlcIndexParser.parse("""
                {
                  "files": [
                    {
                      "path": "../evil.jar",
                      "hashes": { "sha1": "%s" },
                      "downloads": [ "https://cdn.modrinth.com/data/x/evil.jar" ]
                    },
                    {
                      "path": "mods/no-hash.jar",
                      "hashes": {},
                      "downloads": [ "https://cdn.modrinth.com/data/x/no-hash.jar" ]
                    },
                    {
                      "path": "mods/github-only.jar",
                      "hashes": { "sha1": "%s" },
                      "downloads": [ "https://github.com/x/y/mod.jar" ]
                    }
                  ]
                }
                """.formatted(SHA1, SHA1));

        assertFalse(parsed.failed());
        assertTrue(parsed.files().isEmpty());
        assertEquals(3, parsed.skipped());
    }

    @Test
    void missingFilesArrayIsLocalPackNotFailure() {
        DlcIndexFile parsed = DlcIndexParser.parse("{ \"name\": \"local\" }");

        assertFalse(parsed.failed());
        assertTrue(parsed.files().isEmpty());
        assertEquals(0, parsed.skipped());
    }

    @Test
    void emptyFilesArrayIsLocalPack() {
        DlcIndexFile parsed = DlcIndexParser.parse("{ \"files\": [] }");

        assertFalse(parsed.failed());
        assertTrue(parsed.files().isEmpty());
    }

    @Test
    void badJsonDoesNotThrow() {
        DlcIndexFile parsed = DlcIndexParser.parse("{ not json");

        assertTrue(parsed.failed());
        assertTrue(parsed.files().isEmpty());
    }

    @Test
    void missingFileSizeIsMinusOneAndSha1AloneIsEnough() {
        DlcIndexFile parsed = DlcIndexParser.parse("""
                {
                  "files": [
                    {
                      "path": "mods/a.jar",
                      "hashes": { "sha1": "%s" },
                      "downloads": [ "https://cdn.modrinth.com/data/x/a.jar" ]
                    }
                  ]
                }
                """.formatted(SHA1));

        DlcRemoteFile file = parsed.files().get(0);
        assertEquals(-1L, file.fileSize());
        assertEquals(null, file.sha512());
        assertEquals(SHA1, file.sha1());
    }

    @Test
    void parsePackPrefersIndexJsonOverModrinthAndIgnoresNested() throws IOException {
        Path packDir = tempDir.resolve("vietnam");
        Files.createDirectories(packDir.resolve("nested"));
        Files.writeString(packDir.resolve("nested/index.json"), "{ \"files\": [] }");
        Files.writeString(packDir.resolve("zzz-index.json"), filesJson("mods/other.jar"));
        Files.writeString(packDir.resolve("modrinth.index.json"), filesJson("mods/modrinth.jar"));
        Files.writeString(packDir.resolve("index.json"), filesJson("mods/preferred.jar"));

        Optional<Path> found = DlcIndexLocator.find(packDir);
        assertTrue(found.isPresent());
        assertEquals("index.json", found.get().getFileName().toString());

        DlcIndexFile parsed = DlcIndexParser.parsePack(packDir);
        assertEquals(List.of("mods/preferred.jar"), parsed.files().stream().map(DlcRemoteFile::path).toList());
        assertEquals("index.json", parsed.source().getFileName().toString());
    }

    @Test
    void locatorFallsBackToModrinthThenSortedName() throws IOException {
        Path packDir = tempDir.resolve("vietnam");
        Files.createDirectories(packDir);
        Files.writeString(packDir.resolve("zzz-index.json"), "{ \"files\": [] }");
        Files.writeString(packDir.resolve("aaa-index.json"), "{ \"files\": [] }");
        Files.writeString(packDir.resolve("modrinth.index.json"), "{ \"files\": [] }");

        assertEquals("modrinth.index.json", DlcIndexLocator.find(packDir).orElseThrow().getFileName().toString());

        Files.delete(packDir.resolve("modrinth.index.json"));
        assertEquals("aaa-index.json", DlcIndexLocator.find(packDir).orElseThrow().getFileName().toString());
    }

    @Test
    void locatorIgnoresSubdirectoryIndexAndMissingDir() throws IOException {
        Path packDir = tempDir.resolve("vietnam");
        Files.createDirectories(packDir.resolve("nested"));
        Files.writeString(packDir.resolve("nested/index.json"), "{ \"files\": [] }");

        assertTrue(DlcIndexLocator.find(packDir).isEmpty());
        assertTrue(DlcIndexLocator.find(tempDir.resolve("missing")).isEmpty());

        DlcIndexFile parsed = DlcIndexParser.parsePack(packDir);
        assertFalse(parsed.failed());
        assertEquals(null, parsed.source());
        assertTrue(parsed.files().isEmpty());
    }

    private static String filesJson(String path) {
        return """
                {
                  "files": [
                    {
                      "path": "%s",
                      "hashes": { "sha1": "%s" },
                      "downloads": [ "https://cdn.modrinth.com/data/x/file.jar" ]
                    }
                  ]
                }
                """.formatted(path, SHA1);
    }
}
