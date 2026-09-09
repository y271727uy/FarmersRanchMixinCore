package com.y271727uy.FRMC.client.netmusic;

import com.github.tartaricacid.netmusic.client.audio.MusicPlayManager;
import com.github.tartaricacid.netmusic.client.audio.NetMusicSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;

import java.util.function.Consumer;

/**
 * Thin client-only adapter around NetMusic. It deliberately never sends a
 * network message, so the created sound exists only in this Minecraft client.
 */
public final class NetMusicLocalPlayback {
    private NetMusicLocalPlayback() {}

    public static void play(PlaylistTrack track, Consumer<SoundInstance> onCreated) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos listenerPos = minecraft.player == null ? BlockPos.ZERO : minecraft.player.blockPosition();
        MusicPlayManager.play(track.url(), track.name(), url -> {
            SoundInstance sound = new NetMusicSound(listenerPos, url, track.durationSeconds(), null);
            onCreated.accept(sound);
            return sound;
        });
    }
}
