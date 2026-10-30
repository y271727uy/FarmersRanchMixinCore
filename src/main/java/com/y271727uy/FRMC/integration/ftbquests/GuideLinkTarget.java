package com.y271727uy.FRMC.integration.ftbquests;

import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Optional;

/**
 * A GuideME destination written in an FTB quest description.
 * Folders keep their trailing slash so they are not mistaken for pages.
 */
public record GuideLinkTarget(ResourceLocation guideId, String pagePath, String anchor, boolean folder) {
    public static final int MAX_LENGTH = 256;

    public static Optional<GuideLinkTarget> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String value = raw.trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH || value.indexOf(' ') >= 0 || value.indexOf('#') != value.lastIndexOf('#')) {
            return Optional.empty();
        }

        String anchor = "";
        int anchorAt = value.indexOf('#');
        if (anchorAt >= 0) {
            anchor = value.substring(anchorAt + 1);
            value = value.substring(0, anchorAt);
            if (!validAnchor(anchor)) {
                return Optional.empty();
            }
        }

        boolean folder = value.endsWith("/");
        if (folder) {
            if (!anchor.isEmpty()) {
                return Optional.empty();
            }
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty() || value.endsWith("/") || value.contains("//")) {
            return Optional.empty();
        }

        int split = value.indexOf(':');
        if (split <= 0 || split != value.lastIndexOf(':') || split == value.length() - 1) {
            return Optional.empty();
        }
        String namespace = value.substring(0, split);
        String path = value.substring(split + 1);
        int pageSplit = path.indexOf('/');
        if (!validPart(namespace) || !validPart(path.substring(0, pageSplit < 0 ? path.length() : pageSplit))) {
            return Optional.empty();
        }

        String guidePath = pageSplit < 0 ? path : path.substring(0, pageSplit);
        String pagePath = pageSplit < 0 ? "" : path.substring(pageSplit + 1);
        if (folder && pagePath.isEmpty()) {
            return Optional.empty();
        }
        try {
            validate(pagePath);
            return Optional.of(new GuideLinkTarget(new ResourceLocation(namespace, guidePath), pagePath, anchor, folder));
        } catch (ResourceLocationException | IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static void validate(String pagePath) {
        if (pagePath.isEmpty()) {
            return;
        }
        for (String part : pagePath.split("/", -1)) {
            if (!validPart(part)) {
                throw new IllegalArgumentException(part);
            }
        }
    }

    private static boolean validPart(String value) {
        return !value.isEmpty() && ResourceLocation.isValidPath(value) && value.indexOf('.') < 0;
    }

    private static boolean validAnchor(String value) {
        if (value.isEmpty() || value.length() > 64) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '-';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    public String guideKey() {
        return guideId.toString();
    }

    public String display() {
        StringBuilder builder = new StringBuilder(guideKey());
        if (!pagePath.isEmpty()) {
            builder.append('/').append(pagePath);
        }
        if (folder) {
            builder.append('/');
        } else if (!anchor.isEmpty()) {
            builder.append('#').append(anchor);
        }
        return builder.toString();
    }

    public String normalized() {
        return display().toLowerCase(Locale.ROOT);
    }
}
