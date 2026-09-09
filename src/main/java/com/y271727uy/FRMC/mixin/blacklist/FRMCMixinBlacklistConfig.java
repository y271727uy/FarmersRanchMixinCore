package com.y271727uy.FRMC.mixin.blacklist;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;
/*
* 黑名单专用类
*/
public final class FRMCMixinBlacklistConfig {
    private static final Logger LOGGER = Logger.getLogger(FRMCMixinBlacklistConfig.class.getName());
    private static final Pattern BLACKLIST_PATTERN = Pattern.compile("(?s)blacklisted_mixins\\s*=\\s*\\[(.*?)]");
    private static final Pattern STRING_PATTERN = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");

    private static volatile ParsedBlacklist parsedBlacklist = ParsedBlacklist.empty();
    private static volatile boolean loaded;

    // Runtime entries added via addBlacklistedMixin; checked together with the file-based blacklist.
    private static final Set<String> DYNAMIC_EXACT_MIXINS = ConcurrentHashMap.newKeySet();
    private static final Set<String> DYNAMIC_PACKAGE_PREFIXES = ConcurrentHashMap.newKeySet();

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
                Path configPath = configPath();
                ensureConfigExists(configPath);
                parsedBlacklist = parse(Files.readString(configPath, StandardCharsets.UTF_8));
                LOGGER.info(
                    "Loaded FRMC mixin blacklist from "
                        + configPath.toAbsolutePath()
                        + " ("
                        + parsedBlacklist.exactMixins().size()
                        + " exact entries, "
                        + parsedBlacklist.packagePrefixes().size()
                        + " wildcard prefixes)"
                );
            } catch (Exception exception) {
                parsedBlacklist = ParsedBlacklist.empty();
                LOGGER.log(Level.SEVERE, "Failed to load FRMC mixin blacklist", exception);
            }

            loaded = true;
        }
    }

    public static boolean isBlacklisted(String mixinClassName) {
        load();
        return parsedBlacklist.matches(mixinClassName) || matchesDynamic(mixinClassName);
    }

    /**
     * Dynamically blacklists a mixin at runtime. External mods depending on FRMC
     * can call this to cancel a mixin without editing the config file.
     * Supports exact class names and prefix wildcards ending with *.
     *
     * @return true if the entry was newly added, false if it was already blacklisted or invalid
     */
    public static boolean addBlacklistedMixin(String mixinClassName) {
        if (mixinClassName == null || mixinClassName.isBlank()) {
            return false;
        }

        String entry = mixinClassName.trim();
        boolean added;
        if (entry.endsWith("*")) {
            added = DYNAMIC_PACKAGE_PREFIXES.add(entry.substring(0, entry.length() - 1));
        } else {
            added = DYNAMIC_EXACT_MIXINS.add(entry);
        }

        if (added) {
            LOGGER.info("Dynamically blacklisted mixin: " + entry);
        }
        return added;
    }

    static boolean matchesDynamic(String mixinClassName) {
        if (DYNAMIC_EXACT_MIXINS.contains(mixinClassName)) {
            return true;
        }

        for (String prefix : DYNAMIC_PACKAGE_PREFIXES) {
            if (mixinClassName.startsWith(prefix)) {
                return true;
            }
        }

        return false;
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

    private static Path configPath() {
        return com.y271727uy.FRMC.config.FRMCConfigPaths.resolve("frmc-mixin-blacklist.toml");
    }

    private static void ensureConfigExists(Path configPath) throws IOException {
        Path directory = configPath.getParent();
        if (directory != null) {
            Files.createDirectories(directory);
        }

        if (Files.exists(configPath)) {
            return;
        }

        Files.writeString(configPath, defaultConfig(), StandardCharsets.UTF_8);
        LOGGER.info("Created default FRMC mixin blacklist config at " + configPath.toAbsolutePath());
    }

    private static String defaultConfig() {
        return """
            # FRMC mixin blacklist
            # Add fully-qualified mixin class names here to stop them from applying.
            # Supports exact class names and prefix wildcards ending with *.
            # Mixins can also be blacklisted at runtime via
            # FRMCMixinBlacklistConfig.addBlacklistedMixin(...).
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





