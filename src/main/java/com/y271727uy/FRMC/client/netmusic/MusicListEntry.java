package com.y271727uy.FRMC.client.netmusic;

/** One NetEase Cloud Music song reference read from music-list.txt. */
public record MusicListEntry(long id, String name, int lineNumber, Kind kind) {
    public MusicListEntry {
        if (id <= 0) {
            throw new IllegalArgumentException("A music-list entry must have a positive song id");
        }
        kind = kind == null ? Kind.SONG : kind;
        name = name == null || name.isBlank() ? "NetEase " + kind.name().toLowerCase() + " " + id : name;
        lineNumber = Math.max(1, lineNumber);
    }

    public enum Kind {
        SONG,
        DJ
    }
}
