package com.y271727uy.FRMC.client.netmusic;

import com.y271727uy.FRMC.FRMCMod;
import com.y271727uy.FRMC.integration.netmusic.NetMusicIntegration;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.atomic.AtomicBoolean;

/** Client commands for the file-backed, local NetMusic playlist. */
@Mod.EventBusSubscriber(modid = FRMCMod.MODID, value = Dist.CLIENT)
public final class OnlineMusicClientEvents {
    private static final AtomicBoolean MAIN_MENU_LOAD_REQUESTED = new AtomicBoolean();

    private OnlineMusicClientEvents() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("frmc_music")
                .then(Commands.literal("reload").executes(context -> reload(context, false)))
                .then(Commands.literal("reload_play").executes(context -> reload(context, true)))
                .then(Commands.literal("play").executes(context -> play(context)))
                .then(Commands.literal("stop").executes(context -> stop(context)))
                .then(Commands.literal("next").executes(context -> next(context)))
                .then(Commands.literal("previous").executes(context -> previous(context))));
    }

    @SubscribeEvent
    public static void onMainMenuInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof net.minecraft.client.gui.screens.TitleScreen)
                || !NetMusicIntegration.isEnabled()
                || !MAIN_MENU_LOAD_REQUESTED.compareAndSet(false, true)) {
            return;
        }
        NetMusicPlaylistLoader.loadAsync().whenComplete((result, error) ->
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    if (error != null || result == null || result.tracks().isEmpty()) {
                        return;
                    }
                    ClientMusicPlaybackManager manager = ClientMusicPlaybackManager.getInstance();
                    if (manager.playlist().isEmpty()) {
                        manager.setPlaylist(result.tracks());
                    }
                    manager.play();
                }));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MAIN_MENU_LOAD_REQUESTED.set(false);
    }

    private static int reload(CommandContext<CommandSourceStack> context, boolean playAfter) {
        CommandSourceStack source = context.getSource();
        if (!NetMusicIntegration.isAvailable()) {
            source.sendFailure(Component.translatable("frmc.online_music.command.unavailable"));
            return 0;
        }
        NetMusicPlaylistLoader.loadAsync().whenComplete((result, error) ->
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    if (error != null || result == null) {
                        source.sendFailure(Component.translatable("frmc.online_music.command.reload_failed",
                                NetMusicPlaylistLoader.file().toString()));
                        return;
                    }
                    ClientMusicPlaybackManager.getInstance().setPlaylist(result.tracks());
                    source.sendSuccess(() -> Component.translatable("frmc.online_music.command.reloaded",
                            result.tracks().size(), result.parsed().invalidUrls() + result.failedEntries()), false);
                    if (playAfter) {
                        ClientMusicPlaybackManager.getInstance().play();
                    }
                }));
        return 1;
    }

    private static int play(CommandContext<CommandSourceStack> context) {
        if (!checkAvailable(context)) {
            return 0;
        }
        ClientMusicPlaybackManager manager = ClientMusicPlaybackManager.getInstance();
        if (manager.playlist().isEmpty()) {
            context.getSource().sendFailure(Component.translatable("frmc.online_music.command.empty"));
            return 0;
        }
        manager.play();
        context.getSource().sendSuccess(() -> Component.translatable("frmc.online_music.command.playing"), false);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        ClientMusicPlaybackManager.getInstance().stop();
        context.getSource().sendSuccess(() -> Component.translatable("frmc.online_music.command.stopped"), false);
        return 1;
    }

    private static int next(CommandContext<CommandSourceStack> context) {
        if (!checkAvailable(context)) {
            return 0;
        }
        ClientMusicPlaybackManager.getInstance().next();
        return 1;
    }

    private static int previous(CommandContext<CommandSourceStack> context) {
        if (!checkAvailable(context)) {
            return 0;
        }
        ClientMusicPlaybackManager.getInstance().previous();
        return 1;
    }

    private static boolean checkAvailable(CommandContext<CommandSourceStack> context) {
        if (!NetMusicIntegration.isAvailable()) {
            context.getSource().sendFailure(Component.translatable("frmc.online_music.command.unavailable"));
            return false;
        }
        return true;
    }
}
