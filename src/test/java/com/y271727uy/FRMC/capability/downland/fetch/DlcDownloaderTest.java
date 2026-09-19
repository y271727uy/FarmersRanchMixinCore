package com.y271727uy.FRMC.capability.downland.fetch;

import com.y271727uy.FRMC.capability.downland.index.DlcIndexFile;
import com.y271727uy.FRMC.capability.downland.index.DlcIndexParser;
import com.y271727uy.FRMC.capability.downland.index.DlcRemoteFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DlcDownloaderTest {
    private static final byte[] BODY = "hello-dlc".getBytes(StandardCharsets.UTF_8);
    private static final String SHA1 = hash("SHA-1", BODY);
    private static final String SHA512 = hash("SHA-512", BODY);
    private static final String MODRINTH = "https://cdn.modrinth.com/data/x/a.jar";
    private static final String EDGE = "https://edge.forgecdn.net/files/1/a.jar";
    private static final String MEDIA = "https://mediafilez.forgecdn.net/files/1/a.jar";
    private static final String GITHUB = "https://github.com/x/y/a.jar";

    @TempDir
    Path tempDir;

    private Path packDir;

    @BeforeEach
    void setUp() throws IOException {
        packDir = tempDir.resolve("dlc").resolve("vietnam");
        Files.createDirectories(packDir);
    }

    @Test
    void usesFirstWhitelistedUrlAndSkipsGithub() throws IOException {
        RecordingClient http = new RecordingClient();
        http.bodies.put(MODRINTH, BODY);
        http.bodies.put(EDGE, BODY);
        http.bodies.put(MEDIA, BODY);

        DlcDownloader.Result result = DlcDownloader.download(
                packDir,
                index(remote("mods/a.jar", List.of(GITHUB, MODRINTH, EDGE, MEDIA))),
                http
        );

        assertEquals(1, result.downloaded());
        assertEquals(0, result.failed());
        assertEquals(List.of(MODRINTH), http.fetched);
        assertArrayEquals(BODY, Files.readAllBytes(packDir.resolve("mods/a.jar")));
        assertFalse(Files.exists(DlcDownloader.partPath(packDir.resolve("mods/a.jar"))));
    }

    @Test
    void githubOnlyIsRejectedWithoutFetch() {
        RecordingClient http = new RecordingClient();
        http.bodies.put(GITHUB, BODY);

        DlcDownloader.Result result = DlcDownloader.download(
                packDir,
                index(remote("mods/a.jar", List.of(GITHUB))),
                http
        );

        assertEquals(1, result.failed());
        assertEquals(0, result.downloaded());
        assertTrue(http.fetched.isEmpty());
        assertEquals("blocked url", result.files().get(0).detail());
        assertFalse(Files.exists(packDir.resolve("mods/a.jar")));
    }

    @Test
    void matchingDestIsSkippedAndPartIsCleared() throws IOException {
        Path dest = packDir.resolve("mods/a.jar");
        Path part = DlcDownloader.partPath(dest);
        Files.createDirectories(dest.getParent());
        Files.write(dest, BODY);
        Files.write(part, "stale-part".getBytes(StandardCharsets.UTF_8));
        RecordingClient http = new RecordingClient();
        http.bodies.put(MODRINTH, BODY);

        DlcDownloader.Result result = DlcDownloader.download(
                packDir,
                index(remote("mods/a.jar", List.of(MODRINTH))),
                http
        );

        assertEquals(1, result.alreadyPresent());
        assertEquals(0, result.downloaded());
        assertTrue(http.fetched.isEmpty());
        assertFalse(Files.exists(part));
        assertArrayEquals(BODY, Files.readAllBytes(dest));
    }

    @Test
    void badHashDestIsRedownloaded() throws IOException {
        Path dest = packDir.resolve("mods/a.jar");
        Files.createDirectories(dest.getParent());
        Files.writeString(dest, "corrupt");
        RecordingClient http = new RecordingClient();
        http.bodies.put(MODRINTH, BODY);

        DlcDownloader.Result result = DlcDownloader.download(
                packDir,
                index(remote("mods/a.jar", List.of(MODRINTH))),
                http
        );

        assertEquals(1, result.downloaded());
        assertEquals(List.of(MODRINTH), http.fetched);
        assertArrayEquals(BODY, Files.readAllBytes(dest));
        assertFalse(Files.exists(DlcDownloader.partPath(dest)));
    }

    @Test
    void failedFetchDeletesPartAndTriesNextUrl() throws IOException {
        RecordingClient http = new RecordingClient();
        http.errors.put(MODRINTH, new IOException("boom"));
        http.bodies.put(EDGE, BODY);

        DlcDownloader.Result result = DlcDownloader.download(
                packDir,
                index(remote("mods/a.jar", List.of(MODRINTH, EDGE))),
                http
        );

        assertEquals(1, result.downloaded());
        assertEquals(List.of(MODRINTH, EDGE), http.fetched);
        assertArrayEquals(BODY, Files.readAllBytes(packDir.resolve("mods/a.jar")));
        assertFalse(Files.exists(DlcDownloader.partPath(packDir.resolve("mods/a.jar"))));
    }

    @Test
    void hashMismatchDeletesPartAndDoesNotPromote() {
        RecordingClient http = new RecordingClient();
        http.bodies.put(MODRINTH, "not-the-bytes".getBytes(StandardCharsets.UTF_8));

        DlcDownloader.Result result = DlcDownloader.download(
                packDir,
                index(remote("mods/a.jar", List.of(MODRINTH))),
                http
        );

        assertEquals(1, result.failed());
        assertEquals("hash mismatch", result.files().get(0).detail());
        assertFalse(Files.exists(packDir.resolve("mods/a.jar")));
        assertFalse(Files.exists(DlcDownloader.partPath(packDir.resolve("mods/a.jar"))));
    }

    @Test
    void zipSlipPathFailsWithoutFetch() {
        RecordingClient http = new RecordingClient();
        http.bodies.put(MODRINTH, BODY);

        DlcDownloader.Result result = DlcDownloader.download(
                packDir,
                index(new DlcRemoteFile("../evil.jar", List.of(MODRINTH), SHA512, SHA1, BODY.length)),
                http
        );

        assertEquals(1, result.failed());
        assertEquals("path escape", result.files().get(0).detail());
        assertTrue(http.fetched.isEmpty());
        assertFalse(Files.exists(tempDir.resolve("evil.jar")));
        assertFalse(Files.exists(tempDir.resolve("dlc").resolve("evil.jar")));
    }

    @Test
    void fileSizeMismatchFailsAndLeavesNoDest() {
        RecordingClient http = new RecordingClient();
        http.bodies.put(MODRINTH, BODY);

        DlcDownloader.Result result = DlcDownloader.download(
                packDir,
                index(new DlcRemoteFile("mods/a.jar", List.of(MODRINTH), SHA512, SHA1, BODY.length + 1L)),
                http
        );

        assertEquals(1, result.failed());
        assertEquals("hash mismatch", result.files().get(0).detail());
        assertFalse(Files.exists(packDir.resolve("mods/a.jar")));
        assertFalse(Files.exists(DlcDownloader.partPath(packDir.resolve("mods/a.jar"))));
    }

    @Test
    void installJsonPathIsBlocked() {
        RecordingClient http = new RecordingClient();
        http.bodies.put(MODRINTH, BODY);

        DlcDownloader.Result result = DlcDownloader.download(
                packDir,
                index(remote("install.json", List.of(MODRINTH))),
                http
        );

        assertEquals(1, result.failed());
        assertEquals("blocked path", result.files().get(0).detail());
        assertTrue(http.fetched.isEmpty());
    }

    @Test
    void parserDropsEntriesWithoutHashSoDownloaderNeverSeesThem() {
        DlcIndexFile parsed = DlcIndexParser.parse("""
                {
                  "files": [
                    {
                      "path": "mods/no-hash.jar",
                      "hashes": {},
                      "downloads": [ "%s" ]
                    }
                  ]
                }
                """.formatted(MODRINTH));

        assertFalse(parsed.failed());
        assertTrue(parsed.files().isEmpty());
        assertEquals(1, parsed.skipped());

        RecordingClient http = new RecordingClient();
        DlcDownloader.Result result = DlcDownloader.download(packDir, parsed, http);
        assertEquals(0, result.downloaded() + result.alreadyPresent() + result.failed());
        assertTrue(http.fetched.isEmpty());
    }

    @Test
    void asyncWithInlineExecutorMatchesSync() throws IOException {
        RecordingClient http = new RecordingClient();
        http.bodies.put(MODRINTH, BODY);

        DlcDownloader.Result result = DlcDownloader.downloadAsync(
                packDir,
                index(remote("mods/a.jar", List.of(MODRINTH))),
                http,
                Runnable::run
        ).join();

        assertEquals(1, result.downloaded());
        assertTrue(result.ok());
        assertArrayEquals(BODY, Files.readAllBytes(packDir.resolve("mods/a.jar")));
    }

    @Test
    void emptyOrFailedIndexDownloadsNothing() {
        RecordingClient http = new RecordingClient();
        assertEquals(0, DlcDownloader.download(packDir, DlcIndexFile.absent(), http).downloaded());
        assertEquals(0, DlcDownloader.download(packDir, DlcIndexFile.failed(null), http).failed());
        assertTrue(http.fetched.isEmpty());
    }

    private static DlcIndexFile index(DlcRemoteFile... files) {
        return DlcIndexFile.parsed(null, List.of(files), 0);
    }

    private static DlcRemoteFile remote(String path, List<String> downloads) {
        return new DlcRemoteFile(path, downloads, SHA512, SHA1, BODY.length);
    }

    private static String hash(String algorithm, byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes)).toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class RecordingClient implements DlcHttpClient {
        final List<String> fetched = new ArrayList<>();
        final Map<String, byte[]> bodies = new LinkedHashMap<>();
        final Map<String, IOException> errors = new LinkedHashMap<>();

        @Override
        public void fetch(String url, Path dest) throws IOException {
            fetched.add(url);
            IOException error = errors.get(url);
            if (error != null) {
                throw error;
            }
            byte[] body = bodies.get(url);
            if (body == null) {
                throw new IOException("unexpected url " + url);
            }
            Path parent = dest.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(dest, body);
        }
    }
}
