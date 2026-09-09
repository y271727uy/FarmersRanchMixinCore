package com.y271727uy.FRMC.client.netmusic;

import com.y271727uy.FRMC.config.OnlineMusicConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class OnlineMusicSettings {
    private static final OptionInstance<Boolean> SETTINGS_OPTION =
            OptionInstance.createBoolean("frmc.online_music.button", false);

    private OnlineMusicSettings() {}

    public static OptionInstance<Boolean> option() {
        return SETTINGS_OPTION;
    }

    public static boolean isSettingsOption(OptionInstance<?> option) {
        return option == SETTINGS_OPTION;
    }

    public static Button button(int x, int y, int width) {
        return Button.builder(Component.translatable("frmc.online_music.button"), button -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof net.minecraft.client.gui.screens.AccessibilityOptionsScreen parent) {
                minecraft.setScreen(new OnlineMusicSettingsScreen(parent));
            }
        }).bounds(x, y, width, 20).build();
    }
}
