package com.y271727uy.FRMC.client.netmusic;

import com.y271727uy.FRMC.config.FRMCConfigPaths;
import com.y271727uy.FRMC.integration.netmusic.NetMusicIntegration;
import com.github.tartaricacid.netmusic.config.MusicListManage;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Converts the text export into the local queue consumed by NetMusic. */
public final class NetMusicPlaylistLoader {
    private static final int FALLBACK_DURATION_SECONDS = 24 * 60 * 60;

    private NetMusicPlaylistLoader() {}

    public static Path file() {
        return FRMCConfigPaths.resolve("music-list.txt");
    }

    public static LoadResult load() throws IOException {
        MusicListParser.ParseResult parsed = MusicListParser.parse(file());
        if (!NetMusicIntegration.isAvailable()) {
            return new LoadResult(List.of(), parsed, false, 0);
        }
        List<PlaylistTrack> tracks = new ArrayList<>(parsed.entries().size());
        int failedEntries = 0;
        for (MusicListEntry entry : parsed.entries()) {
            if (entry.kind() == MusicListEntry.Kind.SONG) {
                tracks.add(new PlaylistTrack(NetMusicIntegration.songUrl(entry.id()), entry.name(),
                        FALLBACK_DURATION_SECONDS));
                continue;
            }
            try {
                ItemMusicCD.SongInfo song = MusicListManage.getDjSong(entry.id());
                if (song == null || song.songUrl == null || song.songUrl.isBlank()) {
                    failedEntries++;
                    continue;
                }
                String name = entry.name().startsWith("NetEase dj ") && song.songName != null
                        && !song.songName.isBlank() ? song.songName : entry.name();
                tracks.add(new PlaylistTrack(song.songUrl, name,
                        song.songTime > 0 ? song.songTime : FALLBACK_DURATION_SECONDS));
            } catch (Exception exception) {
                failedEntries++;
            }
        }
        return new LoadResult(tracks, parsed, true, failedEntries);
    }

    public static CompletableFuture<LoadResult> loadAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return load();
            } catch (IOException exception) {
                throw new java.util.concurrent.CompletionException(exception);
            }
        });
    }

    public record LoadResult(List<PlaylistTrack> tracks, MusicListParser.ParseResult parsed, boolean netMusicAvailable,
                             int failedEntries) {
        public LoadResult {
            tracks = tracks == null ? List.of() : List.copyOf(tracks);
            failedEntries = Math.max(0, failedEntries);
        }
    }
}
