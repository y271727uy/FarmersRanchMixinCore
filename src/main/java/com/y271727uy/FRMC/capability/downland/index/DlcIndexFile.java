package com.y271727uy.FRMC.capability.downland.index;

import java.nio.file.Path;
import java.util.List;

/**
 * Parse result of one index JSON. Never thrown to the UI.
 * {@code failed} is true only for unreadable / invalid JSON.
 * Missing index or empty {@code files[]} is a local-only pack, not a failure.
 */
public record DlcIndexFile(
        Path source,
        List<DlcRemoteFile> files,
        int skipped,
        boolean failed
) {
    public DlcIndexFile {
        files = files == null ? List.of() : List.copyOf(files);
        skipped = Math.max(0, skipped);
    }

    public static DlcIndexFile absent() {
        return new DlcIndexFile(null, List.of(), 0, false);
    }

    public static DlcIndexFile failed(Path source) {
        return new DlcIndexFile(source, List.of(), 0, true);
    }

    public static DlcIndexFile parsed(Path source, List<DlcRemoteFile> files, int skipped) {
        return new DlcIndexFile(source, files, skipped, false);
    }
}
