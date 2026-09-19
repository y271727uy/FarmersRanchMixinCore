package com.y271727uy.FRMC.capability.downland.index;

import java.util.List;

/**
 * One {@code files[]} row after URL whitelist and path checks.
 * {@code fileSize} is {@code -1} when the index omitted it.
 */
public record DlcRemoteFile(
        String path,
        List<String> downloads,
        String sha512,
        String sha1,
        long fileSize
) {
    public DlcRemoteFile {
        path = path == null ? "" : path;
        downloads = downloads == null ? List.of() : List.copyOf(downloads);
    }
}
