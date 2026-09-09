package com.y271727uy.FRMC.client.netmusic;

import com.y271727uy.FRMC.config.OnlineMusicConfig;
import com.y271727uy.FRMC.integration.netmusic.NetMusicIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;

import java.util.List;

/** Owns the local playlist and playback state for one Minecraft client. */
public final class ClientMusicPlaybackManager {
    private static final ClientMusicPlaybackManager INSTANCE = new ClientMusicPlaybackManager();

    private List<PlaylistTrack> playlist = List.of();
    private int currentIndex = -1;
    private SoundInstance activeSound;
    private int activeTicks;
    /** True while FRMC owns the menu-music session started outside a world. */
    private boolean titleMusicSession;

    private ClientMusicPlaybackManager() {}

    public static ClientMusicPlaybackManager getInstance() {
        return INSTANCE;
    }

    public void setPlaylist(List<PlaylistTrack> tracks) {
        stop();
        playlist = List.copyOf(tracks == null ? List.of() : tracks);
        currentIndex = playlist.isEmpty() ? -1 : 0;
    }

    public List<PlaylistTrack> playlist() {
        return playlist;
    }

    public int currentIndex() {
        return currentIndex;
    }

    public boolean isPlaying() {
        return activeSound != null;
    }

    public void play() {
        if (!NetMusicIntegration.isEnabled() || playlist.isEmpty()) {
            return;
        }
        if (Minecraft.getInstance().level == null) {
            titleMusicSession = true;
        }
        if (currentIndex < 0 || currentIndex >= playlist.size()) {
            currentIndex = 0;
        }
        playCurrent();
    }

    public void stop() {
        stopCurrentSound();
        titleMusicSession = false;
    }

    private void stopCurrentSound() {
        if (activeSound != null) {
            Minecraft.getInstance().getSoundManager().stop(activeSound);
            activeSound = null;
        }
        activeTicks = 0;
    }

    public void clear() {
        stop();
        playlist = List.of();
        currentIndex = -1;
    }

    public void next() {
        if (playlist.isEmpty()) {
            return;
        }
        currentIndex = (currentIndex + 1) % playlist.size();
        play();
    }

    public void previous() {
        if (playlist.isEmpty()) {
            return;
        }
        currentIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        play();
    }

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (titleMusicSession && minecraft.level != null) {
            stop();
            return;
        }
        if (activeSound == null) {
            return;
        }
        if (!NetMusicIntegration.isEnabled()) {
            stop();
            return;
        }
        activeTicks++;
        if (activeTicks > 2 && !minecraft.getSoundManager().isActive(activeSound)) {
            activeSound = null;
            activeTicks = 0;
            advanceAfterCompletion();
        }
    }

    /**
     * Used by the MusicManager mixin to keep vanilla menu music from restarting
     * while the FRMC playlist session is active.
     */
    public boolean shouldSuppressVanillaMusic() {
        if (!titleMusicSession) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null || !NetMusicIntegration.isEnabled()) {
            stop();
            return false;
        }
        return true;
    }

    private void playCurrent() {
        stopCurrentSound();
        PlaylistTrack track = playlist.get(currentIndex);
        NetMusicLocalPlayback.play(track, sound -> {
            activeSound = sound;
            activeTicks = 0;
        });
    }

    private void advanceAfterCompletion() {
        if (playlist.isEmpty()) {
            currentIndex = -1;
            return;
        }
        OnlineMusicConfig.PlaybackMode mode = OnlineMusicConfig.PLAYBACK_MODE.get();
        if (mode == OnlineMusicConfig.PlaybackMode.SINGLE_LOOP) {
            playCurrent();
            return;
        }
        if (mode == OnlineMusicConfig.PlaybackMode.RANDOM) {
            currentIndex = playlist.size() == 1 ? 0 : randomNextIndex();
        } else {
            currentIndex = (currentIndex + 1) % playlist.size();
        }
        playCurrent();
    }

    private int randomNextIndex() {
        int next;
        do {
            next = java.util.concurrent.ThreadLocalRandom.current().nextInt(playlist.size());
        } while (next == currentIndex);
        return next;
    }
}
