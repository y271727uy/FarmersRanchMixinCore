package com.y271727uy.FRMC.mixin.blacklist;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;

public final class FRMCMixinBlacklistConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final Path CONFIG_PATH = Paths.get("config", "frmc-mixin-blacklist.toml");

    private static final Pattern BLACKLIST_PATTERN = Pattern.compile("(?s)blacklisted_mixins\\s*=\\s*\\[(.*?)]");
    private static final Pattern STRING_PATTERN = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");

    private static volatile ParsedBlacklist parsedBlacklist = ParsedBlacklist.empty();
    private static volatile boolean loaded;

    private FRMCMixinBlacklistConfig() {
    }

    public static void load() {
        if (loaded) {
            return;
        }

        synchronized (FRMCMixinBlacklistConfig.class) {
            if (loaded) {
                return;
            }

            try {
                ensureConfigExists();
                parsedBlacklist = parse(Files.readString(CONFIG_PATH, StandardCharsets.UTF_8));
                LOGGER.info(
                    "Loaded FRMC mixin blacklist from {} ({} exact entries, {} wildcard prefixes)",
                    CONFIG_PATH.toAbsolutePath(),
                    parsedBlacklist.exactMixins().size(),
                    parsedBlacklist.packagePrefixes().size()
                );
            } catch (Exception exception) {
                parsedBlacklist = ParsedBlacklist.empty();
                LOGGER.error("Failed to load FRMC mixin blacklist from {}", CONFIG_PATH.toAbsolutePath(), exception);
            }

            loaded = true;
        }
    }

    public static boolean isBlacklisted(String mixinClassName) {
        load();
        return parsedBlacklist.matches(mixinClassName);
    }

    static ParsedBlacklist parse(String tomlText) {
        Matcher blacklistMatcher = BLACKLIST_PATTERN.matcher(tomlText);
        if (!blacklistMatcher.find()) {
            return ParsedBlacklist.empty();
        }

        Set<String> exactMixins = new LinkedHashSet<>();
        List<String> packagePrefixes = new ArrayList<>();
        Matcher stringMatcher = STRING_PATTERN.matcher(blacklistMatcher.group(1));
        while (stringMatcher.find()) {
            String entry = unescapeTomlBasicString(stringMatcher.group(1)).trim();
            if (entry.isEmpty()) {
                continue;
            }

            if (entry.endsWith("*")) {
                packagePrefixes.add(entry.substring(0, entry.length() - 1));
            } else {
                exactMixins.add(entry);
            }
        }

        return new ParsedBlacklist(Set.copyOf(exactMixins), List.copyOf(packagePrefixes));
    }

    private static void ensureConfigExists() throws IOException {
        Path directory = CONFIG_PATH.getParent();
        if (directory != null) {
            Files.createDirectories(directory);
        }

        if (Files.exists(CONFIG_PATH)) {
            return;
        }

        Files.writeString(CONFIG_PATH, defaultConfig(), StandardCharsets.UTF_8);
        LOGGER.info("Created default FRMC mixin blacklist config at {}", CONFIG_PATH.toAbsolutePath());
    }

    private static String defaultConfig() {
        return """
            # FRMC mixin blacklist
            # Add fully-qualified mixin class names here to stop them from applying.
            # Supports exact class names and prefix wildcards ending with *.
            # Examples:
            # "example.mod.mixin.SomeSpecificMixin"
            # "example.mod.mixin.problematic.*"
            blacklisted_mixins = [
                # "example.mod.mixin.SomeSpecificMixin",
                # "example.mod.mixin.problematic.*"
            ]
            """;
    }

    private static String unescapeTomlBasicString(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current != '\\') {
                builder.append(current);
                continue;
            }

            if (index + 1 >= value.length()) {
                builder.append('\\');
                break;
            }

            char escaped = value.charAt(++index);
            switch (escaped) {
                case 'b' -> builder.append('\b');
                case 't' -> builder.append('\t');
                case 'n' -> builder.append('\n');
                case 'f' -> builder.append('\f');
                case 'r' -> builder.append('\r');
                case '"' -> builder.append('"');
                case '\\' -> builder.append('\\');
                case 'u' -> {
                    if (index + 4 < value.length()) {
                        String hex = value.substring(index + 1, index + 5);
                        builder.append((char) Integer.parseInt(hex, 16));
                        index += 4;
                    } else {
                        builder.append('u');
                    }
                }
                default -> builder.append(escaped);
            }
        }
        return builder.toString();
    }

    record ParsedBlacklist(Set<String> exactMixins, List<String> packagePrefixes) {
        private static ParsedBlacklist empty() {
            return new ParsedBlacklist(Set.of(), List.of());
        }

        boolean matches(String mixinClassName) {
            if (exactMixins.contains(mixinClassName)) {
                return true;
            }

            for (String prefix : packagePrefixes) {
                if (mixinClassName.startsWith(prefix)) {
                    return true;
                }
            }

            return false;
        }
    }
}





