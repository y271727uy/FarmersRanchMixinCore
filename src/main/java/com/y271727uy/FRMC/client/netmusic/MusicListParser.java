package com.y271727uy.FRMC.client.netmusic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads common plain-text playlist exports without depending on one exporter.
 * The parser intentionally accepts only NetEase single-song and DJ episode
 * URLs, not playlist pages or short share links which require a separate
 * network resolver.
 */
public final class MusicListParser {
    private static final Pattern NETEASE_URL = Pattern.compile(
            "(?i)https?://(?:(?:www|y)\\.)?music\\.163\\.com(?:/|#)[^\\s<>\\\"']+");
    private static final Pattern SONG_ID = Pattern.compile("(?i)[?&#]id=(\\d+)");

    private MusicListParser() {}

    public static ParseResult parse(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new ParseResult(List.of(), 0, 0, false);
        }
        return parseLines(Files.readAllLines(path, StandardCharsets.UTF_8));
    }

    public static ParseResult parseLines(List<String> lines) {
        List<MusicListEntry> entries = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        int invalidUrls = 0;
        int urlCount = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }
            Matcher matcher = NETEASE_URL.matcher(line);
            while (matcher.find()) {
                urlCount++;
                String url = trimTrailingPunctuation(matcher.group());
                Matcher idMatcher = SONG_ID.matcher(url);
                if (!idMatcher.find()) {
                    invalidUrls++;
                    continue;
                }
                try {
                    long id = Long.parseLong(idMatcher.group(1));
                    MusicListEntry.Kind kind = kindOfUrl(url);
                    if (id <= 0 || kind == null || !seenIds.add(id)) {
                        if (id <= 0 || kind == null) {
                            invalidUrls++;
                        }
                        continue;
                    }
                    entries.add(new MusicListEntry(id, labelFor(line, matcher.start(), matcher.end(), id, kind),
                            i + 1, kind));
                } catch (NumberFormatException exception) {
                    invalidUrls++;
                }
            }
        }
        return new ParseResult(List.copyOf(entries), urlCount, invalidUrls, true);
    }

    private static MusicListEntry.Kind kindOfUrl(String url) {
        int queryStart = url.indexOf('?');
        String pathAndFragment = queryStart >= 0 ? url.substring(0, queryStart) : url;
        int fragmentStart = pathAndFragment.indexOf('#');
        if (fragmentStart >= 0) {
            pathAndFragment = pathAndFragment.substring(fragmentStart + 1);
        }
        if (pathAndFragment.matches("(?i).*[/]song(?:[/]|$).*")) {
            return MusicListEntry.Kind.SONG;
        }
        if (pathAndFragment.matches("(?i).*[/]dj(?:[/]|$).*")) {
            return MusicListEntry.Kind.DJ;
        }
        return null;
    }

    private static String labelFor(String line, int urlStart, int urlEnd, long id, MusicListEntry.Kind kind) {
        String label = (line.substring(0, urlStart) + " " + line.substring(urlEnd)).trim();
        label = label.replaceFirst("^[\\s\\[\\]()#*\\d.、-]+", "")
                .replaceFirst("[\\s\\[\\]()#*\\-:：]+$", "")
                .trim();
        return label.isBlank() ? "NetEase " + kind.name().toLowerCase() + " " + id : label;
    }

    private static String trimTrailingPunctuation(String url) {
        int end = url.length();
        while (end > 0 && ")]}>,.;!?，。！？；：》】）".indexOf(url.charAt(end - 1)) >= 0) {
            end--;
        }
        return url.substring(0, end);
    }

    public record ParseResult(List<MusicListEntry> entries, int urlCount, int invalidUrls, boolean filePresent) {
        public ParseResult {
            entries = entries == null ? List.of() : List.copyOf(entries);
            urlCount = Math.max(0, urlCount);
            invalidUrls = Math.max(0, invalidUrls);
        }
    }
}
