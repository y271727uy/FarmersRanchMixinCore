package com.y271727uy.FRMC.client.netmusic;

/** Client-local song data used by the FRMC playlist queue. */
public record PlaylistTrack(String url, String name, int durationSeconds) {
    public PlaylistTrack {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("A playlist track must have a URL");
        }
        name = name == null || name.isBlank() ? url : name;
        durationSeconds = Math.max(1, durationSeconds);
    }
}
