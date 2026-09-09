package com.y271727uy.FRMC.client.netmusic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MusicListParserTest {
    @Test
    void acceptsCommonSingleSongLinksAndKeepsFirstDuplicate() {
        MusicListParser.ParseResult result = MusicListParser.parseLines(List.of(
                "Song A - Artist https://music.163.com/song?id=123",
                "https://music.163.com/#/song?id=456.mp3",
                "[Mobile song](https://y.music.163.com/m/song?id=789)",
                "Radio episode https://music.163.com/#/dj?id=3720311521",
                "Song A again https://music.163.com/song/media/outer/url?id=123.mp3"
        ));

        assertEquals(5, result.urlCount());
        assertEquals(4, result.entries().size());
        assertEquals(123L, result.entries().get(0).id());
        assertEquals("Song A - Artist", result.entries().get(0).name());
        assertEquals(456L, result.entries().get(1).id());
        assertEquals("Mobile song", result.entries().get(2).name());
        assertEquals(3720311521L, result.entries().get(3).id());
        assertEquals(MusicListEntry.Kind.DJ, result.entries().get(3).kind());
    }

    @Test
    void ignoresPlaylistAndShortShareLinks() {
        MusicListParser.ParseResult result = MusicListParser.parseLines(List.of(
                "https://music.163.com/#/playlist?id=123",
                "https://163cn.tv/abc123",
                "not a link"
        ));

        assertTrue(result.entries().isEmpty());
        assertEquals(1, result.invalidUrls());
    }
}
