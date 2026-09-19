package com.y271727uy.FRMC.capability.downland.scan;

import java.util.Locale;

/**
 * Recognition rules for drop-in download archives ({@code .zip} and {@code .mrpack}).
 * Both are zip containers. A file is downloadable only when the root {@code pack.toml}
 * has exactly one {@code [[dlc]]} and the file stem is {@code DLCId} or
 * {@code DLCId-version} with a matching {@code version}.
 */
public final class DownloadPackRules {
    public static final String DIRECTORY_NAME = "download";
    public static final String PACK_TOML = "pack.toml";
    static final int MAX_PACK_TOML_BYTES = 256 * 1024;
    private static final String ZIP_EXTENSION = ".zip";
    private static final String MRPACK_EXTENSION = ".mrpack";

    private DownloadPackRules() {}

    public static boolean isZipFileName(String fileName) {
        return archiveExtensionLength(fileName) > 0;
    }

    public static String zipStem(String fileName) {
        int extensionLength = archiveExtensionLength(fileName);
        if (extensionLength <= 0) {
            return "";
        }
        return fileName.substring(0, fileName.length() - extensionLength);
    }

    public static boolean isRootPackToml(String entryName) {
        return PACK_TOML.equals(entryName);
    }

    public static boolean isValidPackId(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        if (".".equals(id) || "..".equals(id)) {
            return false;
        }
        return id.indexOf('/') < 0 && id.indexOf('\\') < 0;
    }

    /**
     * {@code vietnam.zip} / {@code vietnam.mrpack} match any toml version;
     * {@code vietnam-1.0.0.zip} / {@code vietnam-1.0.0.mrpack} match only when
     * {@code version} is exactly {@code 1.0.0}. Comparison is case-sensitive.
     */
    public static boolean matchesZipName(String fileName, String dlcId, String version) {
        if (!isZipFileName(fileName) || !isValidPackId(dlcId)) {
            return false;
        }
        String stem = zipStem(fileName);
        if (stem.equals(dlcId)) {
            return true;
        }
        return version != null && !version.isEmpty() && stem.equals(dlcId + "-" + version);
    }

    private static int archiveExtensionLength(String fileName) {
        if (fileName == null) {
            return 0;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(MRPACK_EXTENSION) && fileName.length() > MRPACK_EXTENSION.length()) {
            return MRPACK_EXTENSION.length();
        }
        if (lower.endsWith(ZIP_EXTENSION) && fileName.length() > ZIP_EXTENSION.length()) {
            return ZIP_EXTENSION.length();
        }
        return 0;
    }
}
