package com.y271727uy.FRMC.capability.downland.scan;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class DownloadPackToml {
    private DownloadPackToml() {}

    public static ParseResult parse(String toml) {
        Config parsed;
        try {
            parsed = toml == null || toml.isBlank()
                    ? Config.inMemory()
                    : new TomlParser().parse(toml);
        } catch (RuntimeException exception) {
            return ParseResult.failed();
        }

        List<Config> tables = dlcTables(parsed);
        if (tables.isEmpty()) {
            return ParseResult.noDlc();
        }
        if (tables.size() != 1) {
            return ParseResult.multiple();
        }

        ParsedDlc dlc = parseDlc(tables.get(0));
        if (!DownloadPackRules.isValidPackId(dlc.id())) {
            return ParseResult.invalidId(dlc);
        }
        return ParseResult.ok(dlc);
    }

    private static List<Config> dlcTables(Config parsed) {
        Object dlc = parsed.get("dlc");
        if (dlc instanceof List<?> list) {
            List<Config> tables = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Config config) {
                    tables.add(config);
                }
            }
            return tables;
        }
        if (dlc instanceof Config config) {
            return List.of(config);
        }
        return List.of();
    }

    private static ParsedDlc parseDlc(Config table) {
        String id = readString(first(table, "DLCId", "dlcId", "id"));
        String displayName = readString(first(table, "displayName", "name"));
        return new ParsedDlc(
                id,
                displayName,
                readString(table.get("version")),
                readAuthors(table.get("authors")),
                readString(table.get("description")),
                readString(table.get("license")),
                readString(first(table, "modpack_version", "modpackVersion")),
                readString(first(table, "dlc_type", "dlcType")),
                readString(first(table, "icon", "logo"))
        );
    }

    private static Object first(Config table, String... keys) {
        for (String key : keys) {
            Object value = table.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String readString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String readAuthors(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> item == null ? "" : String.valueOf(item).trim())
                    .filter(item -> !item.isEmpty())
                    .collect(Collectors.joining(", "));
        }
        return String.valueOf(value).trim();
    }

    public record ParseResult(DownloadPackIssue issue, ParsedDlc dlc) {
        static ParseResult ok(ParsedDlc dlc) {
            return new ParseResult(null, dlc);
        }

        static ParseResult failed() {
            return new ParseResult(DownloadPackIssue.PARSE_FAILED, null);
        }

        static ParseResult noDlc() {
            return new ParseResult(DownloadPackIssue.NO_DLC, null);
        }

        static ParseResult multiple() {
            return new ParseResult(DownloadPackIssue.MULTIPLE_DLC, null);
        }

        static ParseResult invalidId(ParsedDlc dlc) {
            return new ParseResult(DownloadPackIssue.INVALID_DLC_ID, dlc);
        }

        public boolean ok() {
            return issue == null && dlc != null;
        }
    }
}
