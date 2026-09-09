package com.y271727uy.FRMC.client.netmusic;

import com.y271727uy.FRMC.config.OnlineMusicConfig;
import com.y271727uy.FRMC.integration.netmusic.NetMusicIntegration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class OnlineMusicSettingsScreen extends Screen {
    private final Screen parent;
    private Button enabledButton;
    private Button modeButton;

    public OnlineMusicSettingsScreen(Screen parent) {
        super(Component.translatable("frmc.online_music.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        this.enabledButton = this.addRenderableWidget(Button.builder(enabledText(), button -> {
            OnlineMusicConfig.ENABLED.set(!OnlineMusicConfig.ENABLED.get());
            ClientMusicPlaybackManager manager = ClientMusicPlaybackManager.getInstance();
            if (OnlineMusicConfig.ENABLED.get()) {
                manager.play();
            } else {
                manager.stop();
            }
            button.setMessage(enabledText());
        }).bounds(center - 100, this.height / 2 - 10, 200, 20).build());
        this.modeButton = this.addRenderableWidget(Button.builder(modeText(), button -> {
            OnlineMusicConfig.PlaybackMode[] modes = OnlineMusicConfig.PlaybackMode.values();
            int next = (OnlineMusicConfig.PLAYBACK_MODE.get().ordinal() + 1) % modes.length;
            OnlineMusicConfig.PLAYBACK_MODE.set(modes[next]);
            button.setMessage(modeText());
        }).bounds(center - 100, this.height / 2 + 20, 200, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button ->
                this.minecraft.setScreen(parent)).bounds(center - 100, this.height / 2 + 55, 200, 20).build());
    }

    private Component modeText() {
        return Component.translatable("frmc.online_music.mode", Component.translatable(
                "frmc.online_music.mode." + OnlineMusicConfig.PLAYBACK_MODE.get().name().toLowerCase()));
    }

    private Component enabledText() {
        if (!NetMusicIntegration.isAvailable()) {
            return Component.translatable("frmc.online_music.unavailable");
        }
        return Component.translatable("frmc.online_music.enabled", OnlineMusicConfig.ENABLED.get()
                ? Component.translatable("options.on") : Component.translatable("options.off"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2,  forty(), 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int forty() { return 40; }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
