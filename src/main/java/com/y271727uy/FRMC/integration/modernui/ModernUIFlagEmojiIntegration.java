package com.y271727uy.FRMC.integration.modernui;

import com.mojang.logging.LogUtils;
import icyllis.modernui.graphics.text.EmojiFont;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

public final class ModernUIFlagEmojiIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FlagEmoji[] FLAGS = {
            new FlagEmoji(new int[] {0x1F1ED, 0x1F1F0}, "1f1ed_1f1f0.png")
    };
    private static final Set<String> FLAG_FILES = Set.of("1f1ed_1f1f0.png");
    private static final Set<String> LOGGED_LOOKUPS = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_LOADS = ConcurrentHashMap.newKeySet();

    private ModernUIFlagEmojiIntegration() {
    }

    public static void registerFlagEmojis(EmojiFont emojiFont) {
        if (!((Object) emojiFont instanceof EmojiFontExtension extension)) {
            return;
        }
        for (FlagEmoji flag : FLAGS) {
            if (!hasBundledEmoji("emoji/" + flag.fileName())) {
                continue;
            }
            extension.frmc$registerEmoji(flag.sequence(), flag.fileName(), flag.codePoints());
            LOGGER.info("Registered Modern UI flag emoji {}", flag.fileName());
        }
    }

    public static InputStream openBundledEmoji(String path) throws IOException {
        if (!path.startsWith("emoji/") || !FLAG_FILES.contains(path.substring("emoji/".length()))) {
            return null;
        }
        String resourcePath = "assets/modernui/" + path;
        InputStream classpathStream = ModernUIFlagEmojiIntegration.class.getClassLoader().getResourceAsStream(resourcePath);
        if (classpathStream != null) {
            logLoad(path, "classpath");
            return classpathStream;
        }

        Path sourcePath = Path.of("src", "main", "resources").resolve(resourcePath);
        if (Files.isRegularFile(sourcePath)) {
            logLoad(path, sourcePath.toString());
            return Files.newInputStream(sourcePath);
        }

        Path buildPath = Path.of("build", "resources", "main").resolve(resourcePath);
        if (Files.isRegularFile(buildPath)) {
            logLoad(path, buildPath.toString());
            return Files.newInputStream(buildPath);
        }
        return null;
    }

    public static void recordFlagLookup(char[] text, int start, int limit, int glyphId) {
        String sequence = new String(text, start, limit - start);
        for (FlagEmoji flag : FLAGS) {
            if (flag.sequence().contentEquals(sequence) && LOGGED_LOOKUPS.add(flag.fileName())) {
                LOGGER.info("Modern UI flag emoji lookup {} -> glyph {}", flag.fileName(), glyphId);
                return;
            }
        }
    }

    private static boolean hasBundledEmoji(String path) {
        try (InputStream stream = openBundledEmoji(path)) {
            return stream != null;
        } catch (IOException e) {
            LOGGER.warn("Failed to check bundled Modern UI emoji {}", path, e);
            return false;
        }
    }

    private static void logLoad(String path, String source) {
        if (LOGGED_LOADS.add(path)) {
            LOGGER.info("Modern UI flag emoji image {} loaded from {}", path, source);
        }
    }

    private record FlagEmoji(int[] codePoints, String fileName) {
        String sequence() {
            return new String(codePoints, 0, codePoints.length);
        }
    }
}
