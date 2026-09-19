package com.y271727uy.FRMC.capability.downland.scan;

import java.nio.file.Path;

public final class DownloadPackEntry {
    private final Path path;
    private final String fileName;
    private final DownloadPackIssue issue;
    private final ParsedDlc dlc;

    private DownloadPackEntry(Path path, String fileName, DownloadPackIssue issue, ParsedDlc dlc) {
        this.path = path;
        this.fileName = fileName;
        this.issue = issue;
        this.dlc = dlc;
    }

    public static DownloadPackEntry valid(Path path, String fileName, ParsedDlc dlc) {
        return new DownloadPackEntry(path, fileName, null, dlc);
    }

    public static DownloadPackEntry invalid(Path path, String fileName, DownloadPackIssue issue) {
        return invalid(path, fileName, issue, null);
    }

    public static DownloadPackEntry invalid(Path path, String fileName, DownloadPackIssue issue, ParsedDlc dlc) {
        return new DownloadPackEntry(path, fileName, issue, dlc);
    }

    public Path path() {
        return path;
    }

    public String fileName() {
        return fileName;
    }

    public boolean valid() {
        return issue == null && dlc != null;
    }

    public DownloadPackIssue issue() {
        return issue;
    }

    public ParsedDlc dlc() {
        return dlc;
    }
}
