package com.y271727uy.FRMC.capability.downland.fetch;

import com.y271727uy.FRMC.capability.downland.index.DlcDownloadRules;
import com.y271727uy.FRMC.capability.downland.index.DlcIndexFile;
import com.y271727uy.FRMC.capability.downland.index.DlcRemoteFile;
import com.y271727uy.FRMC.capability.downland.install.DlcPaths;
import com.y271727uy.FRMC.capability.downland.install.DlcUnpacker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Downloads missing {@code files[]} entries into {@code dlc/<id>/}.
 * Existing dest files whose hash (and size, when known) already match are
 * skipped. Incomplete writes go to {@code *.part} and are discarded on failure.
 */
public final class DlcDownloader {
    public static final String PART_SUFFIX = ".part";

    private DlcDownloader() {}

    public enum FileStatus {
        DOWNLOADED,
        ALREADY_PRESENT,
        FAILED
    }

    public record FileResult(String path, FileStatus status, String detail) {
        public FileResult {
            path = path == null ? "" : path;
            detail = detail == null ? "" : detail;
        }
    }

    public record Result(int downloaded, int alreadyPresent, int failed, List<FileResult> files) {
        public Result {
            files = files == null ? List.of() : List.copyOf(files);
        }

        static Result empty() {
            return new Result(0, 0, 0, List.of());
        }

        static Result of(List<FileResult> files) {
            int downloaded = 0;
            int alreadyPresent = 0;
            int failed = 0;
            for (FileResult file : files) {
                switch (file.status()) {
                    case DOWNLOADED -> downloaded++;
                    case ALREADY_PRESENT -> alreadyPresent++;
                    case FAILED -> failed++;
                }
            }
            return new Result(downloaded, alreadyPresent, failed, files);
        }

        public boolean ok() {
            return failed == 0;
        }
    }

    public static CompletableFuture<Result> downloadAsync(
            Path packDir,
            DlcIndexFile index,
            DlcHttpClient http,
            Executor executor
    ) {
        return CompletableFuture.supplyAsync(() -> download(packDir, index, http), executor);
    }

    public static Result download(Path packDir, DlcIndexFile index, DlcHttpClient http) {
        if (packDir == null || http == null || index == null || index.failed() || index.files().isEmpty()) {
            return Result.empty();
        }
        List<FileResult> results = new ArrayList<>(index.files().size());
        for (DlcRemoteFile file : index.files()) {
            results.add(downloadOne(packDir, file, http));
        }
        return Result.of(results);
    }

    static Path partPath(Path dest) {
        return dest.resolveSibling(dest.getFileName().toString() + PART_SUFFIX);
    }

    static FileResult downloadOne(Path packDir, DlcRemoteFile file, DlcHttpClient http) {
        if (file == null) {
            return new FileResult("", FileStatus.FAILED, "missing file");
        }
        String relative = file.path();
        if (relative.isBlank() || DlcPaths.INSTALL_MARKER.equals(relative)) {
            return new FileResult(relative, FileStatus.FAILED, "blocked path");
        }
        Path dest = DlcUnpacker.resolveInside(packDir, relative);
        if (dest == null) {
            return new FileResult(relative, FileStatus.FAILED, "path escape");
        }
        Path part = partPath(dest);
        if (!inside(packDir, part)) {
            return new FileResult(relative, FileStatus.FAILED, "path escape");
        }

        try {
            if (Files.isRegularFile(dest) && matches(dest, file)) {
                Files.deleteIfExists(part);
                return new FileResult(relative, FileStatus.ALREADY_PRESENT, "");
            }
            if (Files.exists(dest)) {
                Files.delete(dest);
            }
            Files.deleteIfExists(part);

            List<String> urls = file.downloads();
            if (urls.isEmpty()) {
                return new FileResult(relative, FileStatus.FAILED, "no url");
            }
            String lastError = "no url";
            for (String url : urls) {
                if (!DlcDownloadRules.isAllowedDownloadUrl(url)) {
                    lastError = "blocked url";
                    continue;
                }
                try {
                    Path parent = dest.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    http.fetch(url, part);
                    if (!Files.isRegularFile(part)) {
                        lastError = "empty download";
                        Files.deleteIfExists(part);
                        continue;
                    }
                    if (!matches(part, file)) {
                        lastError = "hash mismatch";
                        Files.deleteIfExists(part);
                        continue;
                    }
                    movePart(part, dest);
                    return new FileResult(relative, FileStatus.DOWNLOADED, "");
                } catch (IOException exception) {
                    lastError = exception.getMessage() == null || exception.getMessage().isBlank()
                            ? "io error"
                            : exception.getMessage();
                    Files.deleteIfExists(part);
                }
            }
            return new FileResult(relative, FileStatus.FAILED, lastError);
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(part);
            } catch (IOException ignored) {
            }
            String message = exception.getMessage();
            return new FileResult(relative, FileStatus.FAILED, message == null || message.isBlank() ? "io error" : message);
        }
    }

    static boolean matches(Path file, DlcRemoteFile remote) throws IOException {
        if (remote.fileSize() >= 0L && Files.size(file) != remote.fileSize()) {
            return false;
        }
        if (remote.sha512() != null) {
            return remote.sha512().equalsIgnoreCase(hashHex(file, "SHA-512"));
        }
        if (remote.sha1() != null) {
            return remote.sha1().equalsIgnoreCase(hashHex(file, "SHA-1"));
        }
        return false;
    }

    static String hashHex(Path file, String algorithm) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException(algorithm, exception);
        }
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest()).toLowerCase(Locale.ROOT);
    }

    private static void movePart(Path part, Path dest) throws IOException {
        try {
            Files.move(part, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(part, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean inside(Path packDir, Path candidate) {
        Path base = packDir.toAbsolutePath().normalize();
        Path dest = candidate.toAbsolutePath().normalize();
        return dest.startsWith(base) && !dest.equals(base);
    }
}
