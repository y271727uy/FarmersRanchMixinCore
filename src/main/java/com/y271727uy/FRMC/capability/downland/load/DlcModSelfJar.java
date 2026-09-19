package com.y271727uy.FRMC.capability.downland.load;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipFile;

/**
 * Production FRMC.jar path, or {@code null} on an exploded userdev classpath.
 * Kept free of Forge locator types so unit tests can call it.
 * <p>
 * The SERVICE locator no longer re-adds this jar as a GAME candidate. Path
 * extraction still has to tolerate {@code union:} / nested {@code jar:} URLs
 * because tests and recovery still see those CodeSources.
 */
final class DlcModSelfJar {
    private static final String CLASS_FILE_SUFFIX = ".class";

    private DlcModSelfJar() {}

    /**
     * Find the FRMC jar for re-adding to GAME layer. Returns null in exploded userdev.
     */
    static Path findSelfJar() {
        Path jar = locate();
        if (jar != null) {
            return jar;
        }
        // Fallback: scan mods/ for frmc-*.jar
        Path gameDir = resolveGameDirectory();
        return scanInstalledJar(gameDir);
    }

    /**
     * Resolve the game directory from FML loading context.
     */
    static Path resolveGameDirectory() {
        try {
            return net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get();
        } catch (Exception e) {
            return Paths.get(".").toAbsolutePath().normalize();
        }
    }

    static Path locate() {
        return locate(DlcModScan.class);
    }

    static Path locate(Class<?> type) {
        Path path = fromCodeSource(type);
        if (isJar(path)) {
            return path;
        }
        path = fromClassResource(type);
        if (isJar(path)) {
            return path;
        }
        return null;
    }

    /**
     * Last-resort production recovery: open {@code mods/*.jar} and find the
     * archive that actually contains {@code type}. Never used on an exploded
     * classpath — that copy is already claimed by {@code ClasspathLocator}.
     */
    static Path scanInstalledJar(Path gameDir) {
        return scanInstalledJar(gameDir, DlcModScan.class);
    }

    static Path scanInstalledJar(Path gameDir, Class<?> type) {
        if (gameDir == null || type == null) {
            return null;
        }
        String marker = type.getName().replace('.', '/') + CLASS_FILE_SUFFIX;
        List<Path> jars = new ArrayList<>(DlcModScan.listJars(gameDir.resolve(DlcModScan.MODS_FOLDER)));
        jars.sort(Comparator
            .comparing((Path jar) -> startsWithFrmc(jar) ? 0 : 1)
            .thenComparing(jar -> jar.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
        for (Path jar : jars) {
            if (containsEntry(jar, marker)) {
                return jar.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    static boolean isExplodedClasspath() {
        return isExplodedClasspath(DlcModScan.class);
    }

    static boolean isExplodedClasspath(Class<?> type) {
        try {
            CodeSource source = type.getProtectionDomain().getCodeSource();
            if (source != null && isExplodedFileUrl(source.getLocation())) {
                return true;
            }
        } catch (RuntimeException ignored) {
        }
        try {
            return isExplodedFileUrl(type.getResource(type.getSimpleName() + CLASS_FILE_SUFFIX));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    static String describeCodeSource(Class<?> type) {
        try {
            CodeSource source = type.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) {
                return "null";
            }
            return source.getLocation().toExternalForm();
        } catch (RuntimeException exception) {
            return "error:" + exception.getClass().getSimpleName();
        }
    }

    static String describeClassResource(Class<?> type) {
        try {
            URL resource = type.getResource(type.getSimpleName() + CLASS_FILE_SUFFIX);
            return resource == null ? "null" : resource.toExternalForm();
        } catch (RuntimeException exception) {
            return "error:" + exception.getClass().getSimpleName();
        }
    }

    static Path jarPath(URL location) {
        if (location == null) {
            return null;
        }
        try {
            if ("jar".equalsIgnoreCase(location.getProtocol())) {
                try {
                    URL jarFileUrl = ((JarURLConnection) location.openConnection()).getJarFileURL();
                    Path nested = jarPath(jarFileUrl);
                    if (isJar(nested)) {
                        return nested;
                    }
                } catch (IOException | RuntimeException ignored) {
                }
            }
            if ("file".equalsIgnoreCase(location.getProtocol())) {
                Path path = unwrapJarPath(Paths.get(location.toURI()).toAbsolutePath().normalize());
                if (isJar(path)) {
                    return path;
                }
            }
        } catch (URISyntaxException | RuntimeException ignored) {
        }
        return jarPath(location.toExternalForm());
    }

    static Path jarPath(String location) {
        return extractJarFile(location);
    }

    static boolean isJar(Path path) {
        Path candidate = unwrapJarPath(path);
        if (candidate == null) {
            return false;
        }
        try {
            String name = candidate.getFileName() == null ? "" : candidate.getFileName().toString();
            return Files.isRegularFile(candidate) && name.toLowerCase(Locale.ROOT).endsWith(".jar");
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static Path fromCodeSource(Class<?> type) {
        try {
            CodeSource source = type.getProtectionDomain().getCodeSource();
            if (source == null) {
                return null;
            }
            return jarPath(source.getLocation());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Path fromClassResource(Class<?> type) {
        try {
            URL resource = type.getResource(type.getSimpleName() + CLASS_FILE_SUFFIX);
            return resource == null ? null : jarPath(resource);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean isExplodedFileUrl(URL location) {
        if (location == null || !"file".equalsIgnoreCase(location.getProtocol())) {
            return false;
        }
        try {
            Path path = Paths.get(location.toURI());
            return Files.isDirectory(path) || looksLikeClassFile(path);
        } catch (URISyntaxException | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean looksLikeClassFile(Path path) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString();
        return Files.isRegularFile(path) && name.endsWith(CLASS_FILE_SUFFIX);
    }

    private static Path extractJarFile(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String decoded = decodeRepeatedly(raw);
        int idx = indexOfJarExtension(decoded);
        if (idx < 0) {
            return null;
        }
        String upToJar = stripUriSchemes(decoded.substring(0, idx + 4));
        upToJar = trimLeadingSlashesForWindows(upToJar);
        try {
            Path path = Paths.get(upToJar).toAbsolutePath().normalize();
            return isJar(path) ? path : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Path unwrapJarPath(Path path) {
        if (path == null) {
            return null;
        }
        String raw = path.toString();
        int idx = indexOfJarExtension(raw);
        if (idx < 0) {
            return path;
        }
        try {
            return Paths.get(raw.substring(0, idx + 4)).toAbsolutePath().normalize();
        } catch (RuntimeException ignored) {
            return path;
        }
    }

    private static String decodeRepeatedly(String raw) {
        String current = raw;
        for (int i = 0; i < 3; i++) {
            try {
                String next = URLDecoder.decode(current.replace("+", "%2B"), StandardCharsets.UTF_8);
                if (next.equals(current)) {
                    break;
                }
                current = next;
            } catch (RuntimeException ignored) {
                break;
            }
        }
        return current;
    }

    private static String stripUriSchemes(String value) {
        String current = value;
        while (true) {
            int colon = current.indexOf(':');
            if (colon <= 0) {
                return current;
            }
            String scheme = current.substring(0, colon);
            if (scheme.length() == 1 && Character.isLetter(scheme.charAt(0))) {
                return current;
            }
            if (!isUriScheme(scheme)) {
                return current;
            }
            current = current.substring(colon + 1);
            if (current.startsWith("//")) {
                current = current.substring(2);
            }
        }
    }

    private static boolean isUriScheme(String scheme) {
        if (scheme.isEmpty()) {
            return false;
        }
        if (!Character.isLetter(scheme.charAt(0))) {
            return false;
        }
        for (int i = 1; i < scheme.length(); i++) {
            char ch = scheme.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '+' || ch == '.' || ch == '-') {
                continue;
            }
            return false;
        }
        return true;
    }

    private static String trimLeadingSlashesForWindows(String path) {
        String current = path;
        while (current.startsWith("//")) {
            current = current.substring(1);
        }
        if (current.length() >= 3
                && (current.charAt(0) == '/' || current.charAt(0) == '\\')
                && Character.isLetter(current.charAt(1))
                && current.charAt(2) == ':') {
            return current.substring(1);
        }
        return current;
    }

    private static int indexOfJarExtension(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        int from = 0;
        while (from < lower.length()) {
            int idx = lower.indexOf(".jar", from);
            if (idx < 0) {
                return -1;
            }
            int end = idx + 4;
            if (end == lower.length() || !isJarNameChar(lower.charAt(end))) {
                return idx;
            }
            from = end;
        }
        return -1;
    }

    private static boolean isJarNameChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == '.';
    }

    private static boolean startsWithFrmc(Path jar) {
        String name = jar.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.startsWith("frmc") && name.endsWith(".jar");
    }

    private static boolean containsEntry(Path jar, String entry) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            return zip.getEntry(entry) != null;
        } catch (IOException ignored) {
            return false;
        }
    }
}
