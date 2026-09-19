package com.y271727uy.FRMC.capability.downland.index;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.y271727uy.FRMC.capability.downland.install.DlcUnpacker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Reads one index JSON's {@code files[]} array. Other fields are ignored.
 * Bad JSON becomes an empty failed result instead of throwing to the UI.
 */
public final class DlcIndexParser {
    private DlcIndexParser() {}

    public static DlcIndexFile parsePack(Path packDir) {
        Optional<Path> found = DlcIndexLocator.find(packDir);
        if (found.isEmpty()) {
            return DlcIndexFile.absent();
        }
        return parse(found.get(), packDir);
    }

    public static DlcIndexFile parse(Path indexFile, Path packDir) {
        if (indexFile == null) {
            return DlcIndexFile.failed(null);
        }
        try {
            String json = Files.readString(indexFile, StandardCharsets.UTF_8);
            return parse(json, packDir, indexFile);
        } catch (IOException | RuntimeException exception) {
            return DlcIndexFile.failed(indexFile);
        }
    }

    public static DlcIndexFile parse(String json) {
        return parse(json, Path.of("dlc", "pack"), null);
    }

    public static DlcIndexFile parse(String json, Path packDir) {
        return parse(json, packDir, null);
    }

    static DlcIndexFile parse(String json, Path packDir, Path source) {
        if (json == null || json.isBlank()) {
            return DlcIndexFile.failed(source);
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException exception) {
            return DlcIndexFile.failed(source);
        }
        if (root == null || !root.isJsonObject()) {
            return DlcIndexFile.failed(source);
        }

        JsonElement filesElement = root.getAsJsonObject().get("files");
        if (filesElement == null || filesElement.isJsonNull()) {
            return DlcIndexFile.parsed(source, List.of(), 0);
        }
        if (!filesElement.isJsonArray()) {
            return DlcIndexFile.failed(source);
        }

        Path base = packDir == null ? Path.of("dlc", "pack") : packDir;
        List<DlcRemoteFile> files = new ArrayList<>();
        int skipped = 0;
        for (JsonElement element : filesElement.getAsJsonArray()) {
            DlcRemoteFile file = parseFile(element, base);
            if (file == null) {
                skipped++;
            } else {
                files.add(file);
            }
        }
        return DlcIndexFile.parsed(source, files, skipped);
    }

    private static DlcRemoteFile parseFile(JsonElement element, Path packDir) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        String path = posixPath(string(object, "path"));
        if (path.isEmpty() || DlcUnpacker.resolveInside(packDir, path) == null) {
            return null;
        }

        List<String> downloads = allowedDownloads(object.get("downloads"));
        if (downloads.isEmpty()) {
            return null;
        }

        String sha512 = hash(object, "sha512");
        String sha1 = hash(object, "sha1");
        if (sha512 == null && sha1 == null) {
            return null;
        }

        return new DlcRemoteFile(path, downloads, sha512, sha1, fileSize(object));
    }

    private static List<String> allowedDownloads(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        List<String> urls = new ArrayList<>();
        for (JsonElement item : array) {
            if (item == null || !item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) {
                continue;
            }
            String url = item.getAsString();
            if (DlcDownloadRules.isAllowedDownloadUrl(url)) {
                urls.add(url);
            }
        }
        return urls;
    }

    private static String hash(JsonObject file, String algorithm) {
        JsonElement hashes = file.get("hashes");
        if (hashes == null || !hashes.isJsonObject()) {
            return null;
        }
        String value = string(hashes.getAsJsonObject(), algorithm);
        if (value.isEmpty()) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static long fileSize(JsonObject file) {
        JsonElement size = file.get("fileSize");
        if (size == null || !size.isJsonPrimitive() || !size.getAsJsonPrimitive().isNumber()) {
            return -1L;
        }
        try {
            long value = size.getAsLong();
            return value >= 0L ? value : -1L;
        } catch (RuntimeException exception) {
            return -1L;
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return "";
        }
        String value = element.getAsString();
        return value == null ? "" : value.trim();
    }

    private static String posixPath(String path) {
        if (path.isEmpty()) {
            return "";
        }
        String name = path.replace('\\', '/');
        while (name.startsWith("./")) {
            name = name.substring(2);
        }
        return name;
    }
}
