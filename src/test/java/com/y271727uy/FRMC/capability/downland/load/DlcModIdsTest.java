package com.y271727uy.FRMC.capability.downland.load;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class DlcModIdsTest {
    @TempDir
    Path tempDir;

    @Test
    void parseCollectsDistinctModIdsInOrder() {
        List<String> ids = DlcModIds.parse("""
                modLoader="javafml"
                [[mods]]
                modId="alpha"
                [[mods]]
                modId="beta"
                [[mods]]
                modId="alpha"
                """);

        assertEquals(List.of("alpha", "beta"), ids);
    }

    @Test
    void parseAcceptsSingleModsTable() {
        assertEquals(List.of("solo"), DlcModIds.parse("""
                [mods]
                modId="solo"
                """));
    }

    @Test
    void parseSkipsBlankAndBrokenToml() {
        assertEquals(List.of(), DlcModIds.parse("[[mods]]\nmodId=\"\"\n"));
        assertEquals(List.of(), DlcModIds.parse("<<<not toml"));
        assertEquals(List.of(), DlcModIds.parse(""));
        assertEquals(List.of(), DlcModIds.parse(null));
    }

    @Test
    void readReturnsModIdsFromJar() throws IOException {
        Path jar = writeJar("ok.jar", Map.of(
                "META-INF/mods.toml", """
                        [[mods]]
                        modId="vietnamfood"
                        """
        ));

        assertEquals(List.of("vietnamfood"), DlcModIds.read(jar));
    }

    @Test
    void readSkipsMissingTomlAndUnreadableFiles() throws IOException {
        Path noToml = writeJar("empty.jar", Map.of("readme.txt", "nope"));
        Path notZip = tempDir.resolve("plain.jar");
        Files.writeString(notZip, "not a zip");

        assertEquals(List.of(), DlcModIds.read(noToml));
        assertEquals(List.of(), DlcModIds.read(notZip));
        assertEquals(List.of(), DlcModIds.read(null));
    }

    @Test
    void reservedIdsAreCaseInsensitive() {
        assertTrue(DlcModIds.isReserved("forge"));
        assertTrue(DlcModIds.isReserved("Minecraft"));
        assertTrue(DlcModIds.isReserved("FRMC"));
        assertFalse(DlcModIds.isReserved("vietnamfood"));
        assertFalse(DlcModIds.isReserved(null));
    }

    private Path writeJar(String fileName, Map<String, String> entries) throws IOException {
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
