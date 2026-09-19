package com.y271727uy.FRMC.capability.downland.install;

import com.y271727uy.FRMC.capability.downland.scan.DownloadPackEntry;
import com.y271727uy.FRMC.capability.downland.scan.DownloadPackRules;
import com.y271727uy.FRMC.capability.downland.scan.ParsedDlc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Extracts a scanned download archive into {@code dlc/<id>/}.
 * Local unzip only: no network, no {@code index.json} handling.
 */
public final class DlcUnpacker {
    private DlcUnpacker() {}

    public record Result(Status status, Path packDir, String detail) {
        public enum Status {
            SUCCESS,
            ALREADY_INSTALLED,
            REJECTED,
            FAILED
        }

        public static Result success(Path packDir) {
            return new Result(Status.SUCCESS, packDir, "");
        }

        public static Result alreadyInstalled(Path packDir) {
            return new Result(Status.ALREADY_INSTALLED, packDir, "");
        }

        public static Result rejected(Path packDir, String detail) {
            return new Result(Status.REJECTED, packDir, detail == null ? "" : detail);
        }

        public static Result failed(Path packDir, String detail) {
            return new Result(Status.FAILED, packDir, detail == null ? "" : detail);
        }
    }

    public static Result unpack(DownloadPackEntry entry, Path gameDir) {
        if (entry == null || !entry.valid() || entry.dlc() == null) {
            return Result.rejected(null, "pack is not installable");
        }
        return unpack(entry.path(), entry.fileName(), entry.dlc(), gameDir);
    }

    public static Result unpack(Path archive, String fileName, ParsedDlc dlc, Path gameDir) {
        if (dlc == null || !DownloadPackRules.isValidPackId(dlc.id())) {
            return Result.rejected(null, "invalid DLC id");
        }
        if (archive == null || !Files.isRegularFile(archive)) {
            return Result.failed(null, "archive missing");
        }

        Path packDir = DlcPaths.packDir(gameDir, dlc.id());
        if (DlcPaths.isInstalled(packDir)) {
            return Result.alreadyInstalled(packDir);
        }

        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            List<PlannedFile> planned = plan(zip, packDir);
            if (planned == null) {
                return Result.rejected(packDir, "archive contains a path outside dlc/" + dlc.id());
            }

            DlcPaths.ensureRoot(gameDir);
            Files.createDirectories(packDir);
            for (PlannedFile file : planned) {
                Path parent = file.dest.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                try (InputStream in = zip.getInputStream(file.entry);
                     OutputStream out = Files.newOutputStream(
                             file.dest,
                             StandardOpenOption.CREATE,
                             StandardOpenOption.TRUNCATE_EXISTING
                     )) {
                    in.transferTo(out);
                }
            }
            Files.writeString(
                    DlcPaths.installMarker(packDir),
                    installJson(dlc.id(), dlc.version(), fileName),
                    StandardCharsets.UTF_8
            );
            return Result.success(packDir);
        } catch (IOException exception) {
            String message = exception.getMessage();
            return Result.failed(packDir, message == null || message.isBlank() ? "io error" : message);
        }
    }

    private static List<PlannedFile> plan(ZipFile zip, Path packDir) {
        List<PlannedFile> planned = new ArrayList<>();
        var entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory() || isInstallMarker(entry.getName())) {
                continue;
            }
            Path dest = resolveInside(packDir, entry.getName());
            if (dest == null) {
                return null;
            }
            planned.add(new PlannedFile(entry, dest));
        }
        return planned;
    }

    private static boolean isInstallMarker(String entryName) {
        if (entryName == null) {
            return false;
        }
        String name = entryName.replace('\\', '/');
        while (name.startsWith("./")) {
            name = name.substring(2);
        }
        return DlcPaths.INSTALL_MARKER.equals(name);
    }

    public static Path resolveInside(Path packDir, String entryName) {
        if (entryName == null || entryName.isBlank()) {
            return null;
        }
        String name = entryName.replace('\\', '/');
        while (name.startsWith("./")) {
            name = name.substring(2);
        }
        if (name.isBlank() || name.startsWith("/") || name.contains(":")) {
            return null;
        }
        Path base = packDir.toAbsolutePath().normalize();
        Path dest = packDir.resolve(name).toAbsolutePath().normalize();
        if (!dest.startsWith(base) || dest.equals(base)) {
            return null;
        }
        return dest;
    }

    private static String installJson(String id, String version, String source) {
        return "{"
                + "\"id\":" + jsonString(id)
                + ",\"version\":" + jsonString(version == null ? "" : version)
                + ",\"source\":" + jsonString(source == null ? "" : source)
                + "}\n";
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private record PlannedFile(ZipEntry entry, Path dest) {}
}
