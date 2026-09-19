package com.y271727uy.FRMC.capability.downland.load;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DlcModSelfJarTest {
    @TempDir
    Path tempDir;

    @Test
    void explodedClasspathReturnsNull() {
        assertNull(DlcModSelfJar.locate(DlcModSelfJarTest.class));
        assertTrue(DlcModSelfJar.isExplodedClasspath(DlcModSelfJarTest.class));
    }

    @Test
    void fileUrlResolvesHostJar() throws Exception {
        Path jar = writeZip(tempDir.resolve("frmc-1.0.0.jar"), Map.of("marker.txt", "ok"));
        URL location = jar.toUri().toURL();

        assertEquals(jar.toAbsolutePath().normalize(), DlcModSelfJar.jarPath(location));
        assertTrue(DlcModSelfJar.isJar(jar));
    }

    @Test
    void jarResourceUrlResolvesHostJar() throws Exception {
        Path jar = writeZip(tempDir.resolve("frmc.jar"), Map.of("marker.txt", "ok"));
        URL resource = new URL("jar:" + jar.toUri().toURL() + "!/com/example/Marker.class");

        assertEquals(jar.toAbsolutePath().normalize(), DlcModSelfJar.jarPath(resource));
    }

    @Test
    void unionUrlWithEntrySuffixResolvesHostJar() throws Exception {
        Path jar = writeZip(tempDir.resolve("frmc-1.0.0.jar"), Map.of("marker.txt", "ok"));
        Path absolute = jar.toAbsolutePath().normalize();
        String slashPath = absolute.toString().replace('\\', '/');
        if (!slashPath.startsWith("/")) {
            slashPath = "/" + slashPath;
        }

        assertEquals(absolute, DlcModSelfJar.jarPath("union:" + jar.toUri() + "%2314141414!/"));
        assertEquals(absolute, DlcModSelfJar.jarPath("union:" + slashPath + "%2314141414!/"));
    }

    @Test
    void nestedJarUnionUrlResolvesOuterJar() throws Exception {
        Path jar = writeZip(tempDir.resolve("frmc-1.0.0.jar"), Map.of("marker.txt", "ok"));
        String nested = "jar:union:" + jar.toAbsolutePath().toUri()
            + "!/META-INF/jars/nested.jar!/com/example/Marker.class";

        assertEquals(jar.toAbsolutePath().normalize(), DlcModSelfJar.jarPath(nested));
    }

    @Test
    void windowsFileUrlWithoutLeadingSlashResolves() throws Exception {
        Path jar = writeZip(tempDir.resolve("frmc.jar"), Map.of("marker.txt", "ok"));
        String raw = "file:///" + jar.toAbsolutePath().toString().replace('\\', '/');

        assertEquals(jar.toAbsolutePath().normalize(), DlcModSelfJar.jarPath(raw));
    }

    @Test
    void encodedPathWithSpacesResolvesHostJar() throws Exception {
        Path folder = tempDir.resolve("Release 2.6.15");
        Path jar = writeZip(folder.resolve("frmc-1.0.0.jar"), Map.of("marker.txt", "ok"));
        Path absolute = jar.toAbsolutePath().normalize();
        String encoded = jar.toUri().toString().replace(" ", "%20");

        assertEquals(absolute, DlcModSelfJar.jarPath("union:" + encoded + "%2314141414!/"));
        assertEquals(absolute, DlcModSelfJar.jarPath("jar:" + encoded + "!/com/example/Marker.class"));
    }

    @Test
    void explodedDirectoryIsNotAJar() throws IOException {
        Path classes = tempDir.resolve("classes");
        Files.createDirectories(classes);

        assertFalse(DlcModSelfJar.isJar(classes));
        assertNull(DlcModSelfJar.jarPath(classes.toUri().toURL()));
    }

    @Test
    void scanInstalledJarFindsFrmcFirst() throws IOException {
        Path mods = tempDir.resolve("mods");
        writeZip(mods.resolve("other.jar"), Map.of(
            "com/y271727uy/FRMC/capability/downland/load/DlcModScan.class", "no"
        ));
        Path frmc = writeZip(mods.resolve("frmc-1.0.0.jar"), Map.of(
            "com/y271727uy/FRMC/capability/downland/load/DlcModScan.class", "yes"
        ));

        assertEquals(frmc.toAbsolutePath().normalize(), DlcModSelfJar.scanInstalledJar(tempDir, DlcModScan.class));
    }

    @Test
    void scanInstalledJarReturnsNullWhenMarkerIsAbsent() throws IOException {
        writeZip(tempDir.resolve("mods").resolve("frmc-1.0.0.jar"), Map.of("readme.txt", "no locator"));

        assertNull(DlcModSelfJar.scanInstalledJar(tempDir, DlcModScan.class));
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
