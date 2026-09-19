package com.y271727uy.FRMC.capability.downland.index;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks at most one index JSON from the top of {@code dlc/<id>/}.
 * Priority: {@code index.json} → {@code modrinth.index.json} → other
 * {@code *index*.json} names, case-insensitive. Subdirectories are ignored.
 */
public final class DlcIndexLocator {
    private static final String INDEX_JSON = "index.json";
    private static final String MODRINTH_INDEX_JSON = "modrinth.index.json";

    private DlcIndexLocator() {}

    public static Optional<Path> find(Path packDir) {
        if (packDir == null || !Files.isDirectory(packDir)) {
            return Optional.empty();
        }

        List<Path> matches = new ArrayList<>();
        try (DirectoryStream<Path> children = Files.newDirectoryStream(packDir)) {
            for (Path child : children) {
                if (!Files.isRegularFile(child)) {
                    continue;
                }
                if (DlcDownloadRules.isIndexFileName(child.getFileName().toString())) {
                    matches.add(child);
                }
            }
        } catch (IOException ignored) {
            return Optional.empty();
        }

        if (matches.isEmpty()) {
            return Optional.empty();
        }

        Path indexJson = named(matches, INDEX_JSON);
        if (indexJson != null) {
            return Optional.of(indexJson);
        }
        Path modrinth = named(matches, MODRINTH_INDEX_JSON);
        if (modrinth != null) {
            return Optional.of(modrinth);
        }

        matches.sort(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
        return Optional.of(matches.get(0));
    }

    private static Path named(List<Path> matches, String expected) {
        for (Path match : matches) {
            if (expected.equalsIgnoreCase(match.getFileName().toString())) {
                return match;
            }
        }
        return null;
    }
}
