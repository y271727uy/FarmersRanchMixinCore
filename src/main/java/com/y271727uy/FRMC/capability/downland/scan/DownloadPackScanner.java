package com.y271727uy.FRMC.capability.downland.scan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DownloadPackScanner {
    private DownloadPackScanner() {}

    public static List<DownloadPackEntry> scan(Path directory) {
        List<DownloadPackEntry> entries = new ArrayList<>();
        if (directory == null || !Files.isDirectory(directory)) {
            return entries;
        }

        try (DirectoryStream<Path> children = Files.newDirectoryStream(directory)) {
            for (Path child : children) {
                if (!Files.isRegularFile(child)) {
                    continue;
                }
                String fileName = child.getFileName().toString();
                if (!DownloadPackRules.isZipFileName(fileName)) {
                    continue;
                }
                entries.add(inspect(child, fileName));
            }
        } catch (IOException ignored) {
            return entries;
        }

        entries.sort(Comparator.comparing(DownloadPackEntry::fileName, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    static DownloadPackEntry inspect(Path path, String fileName) {
        try (ZipFile zip = new ZipFile(path.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry tomlEntry = rootPackToml(zip);
            if (tomlEntry == null) {
                return DownloadPackEntry.invalid(path, fileName, DownloadPackIssue.MISSING_PACK_TOML);
            }
            if (tomlEntry.getSize() > DownloadPackRules.MAX_PACK_TOML_BYTES) {
                return DownloadPackEntry.invalid(path, fileName, DownloadPackIssue.PACK_TOML_TOO_LARGE);
            }

            String toml = readEntry(zip, tomlEntry);
            if (toml == null) {
                return DownloadPackEntry.invalid(path, fileName, DownloadPackIssue.PACK_TOML_TOO_LARGE);
            }
            DownloadPackToml.ParseResult parsed = DownloadPackToml.parse(toml);
            if (!parsed.ok()) {
                return DownloadPackEntry.invalid(path, fileName, parsed.issue(), parsed.dlc());
            }

            ParsedDlc dlc = parsed.dlc();
            if (!DownloadPackRules.matchesZipName(fileName, dlc.id(), dlc.version())) {
                return DownloadPackEntry.invalid(path, fileName, DownloadPackIssue.NAME_MISMATCH, dlc);
            }
            return DownloadPackEntry.valid(path, fileName, dlc);
        } catch (IOException | RuntimeException exception) {
            return DownloadPackEntry.invalid(path, fileName, DownloadPackIssue.UNREADABLE);
        }
    }

    private static ZipEntry rootPackToml(ZipFile zip) {
        ZipEntry exact = zip.getEntry(DownloadPackRules.PACK_TOML);
        if (exact != null && !exact.isDirectory()) {
            return exact;
        }
        var entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            String name = entry.getName().replace('\\', '/');
            if (name.startsWith("./")) {
                name = name.substring(2);
            }
            if (DownloadPackRules.isRootPackToml(name)) {
                return entry;
            }
        }
        return null;
    }

    private static String readEntry(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream in = zip.getInputStream(entry)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int total = 0;
            int read;
            while ((read = in.read(buffer)) >= 0) {
                total += read;
                if (total > DownloadPackRules.MAX_PACK_TOML_BYTES) {
                    return null;
                }
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8);
        }
    }
}
