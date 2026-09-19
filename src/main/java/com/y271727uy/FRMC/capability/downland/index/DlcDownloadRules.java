package com.y271727uy.FRMC.capability.downland.index;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * Index filename and remote-URL rules for DLC {@code files[]} entries.
 * No HTTP: this only decides what a later downloader is allowed to touch.
 */
public final class DlcDownloadRules {
    public static final String INDEX_NAME_NEEDLE = "index";
    public static final Set<String> ALLOWED_HOSTS = Set.of(
            "cdn.modrinth.com",
            "edge.forgecdn.net",
            "mediafilez.forgecdn.net",
            "media.forgecdn.net"
    );

    private DlcDownloadRules() {}

    public static boolean isIndexFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".json")
                && lower.contains(INDEX_NAME_NEEDLE)
                && lower.length() > ".json".length();
    }

    public static boolean isAllowedDownloadUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        URI uri;
        try {
            uri = URI.create(raw.trim());
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        if (uri.getUserInfo() != null) {
            return false;
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }
        host = host.toLowerCase(Locale.ROOT);
        if (isIpAddress(host)) {
            return false;
        }
        int port = uri.getPort();
        if (port != -1 && port != 443) {
            return false;
        }
        return ALLOWED_HOSTS.contains(host);
    }

    private static boolean isIpAddress(String host) {
        if (host.indexOf(':') >= 0) {
            return true;
        }
        String[] parts = host.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty()) {
                return false;
            }
            try {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            } catch (NumberFormatException exception) {
                return false;
            }
        }
        return true;
    }
}
