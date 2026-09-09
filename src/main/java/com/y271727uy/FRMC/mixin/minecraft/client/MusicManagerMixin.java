package com.y271727uy.FRMC.mixin.minecraft.client;

import com.y271727uy.FRMC.client.netmusic.ClientMusicPlaybackManager;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps vanilla menu music from competing with an FRMC playlist. */
@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {
    private boolean frmc$vanillaMusicStopped;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void frmc$suppressVanillaMenuMusic(CallbackInfo ci) {
        ClientMusicPlaybackManager manager = ClientMusicPlaybackManager.getInstance();
        if (frmc$shouldSuppress(manager)) {
            ci.cancel();
        }
    }

    @Inject(method = "startPlaying", at = @At("HEAD"), cancellable = true)
    private void frmc$blockVanillaMenuMusicStart(Music music, CallbackInfo ci) {
        ClientMusicPlaybackManager manager = ClientMusicPlaybackManager.getInstance();
        if (frmc$shouldSuppress(manager)) {
            ci.cancel();
        }
    }

    private boolean frmc$shouldSuppress(ClientMusicPlaybackManager manager) {
        if (!manager.shouldSuppressVanillaMusic()) {
            frmc$vanillaMusicStopped = false;
            return false;
        }
        if (!frmc$vanillaMusicStopped) {
            ((MusicManager) (Object) this).stopPlaying();
            frmc$vanillaMusicStopped = true;
        }
        return true;
    }
}
