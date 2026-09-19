package com.y271727uy.FRMC.capability.downland.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.AccessibilityOptionsScreen;
import net.minecraft.network.chat.Component;

public final class AdditionalContentPackSettings {
    private static final OptionInstance<Boolean> SETTINGS_OPTION =
            OptionInstance.createBoolean("frmc.additional_content_pack.button", false);

    private AdditionalContentPackSettings() {}

    public static OptionInstance<Boolean> option() {
        return SETTINGS_OPTION;
    }

    public static boolean isSettingsOption(OptionInstance<?> option) {
        return option == SETTINGS_OPTION;
    }

    public static Button button(int x, int y, int width) {
        return Button.builder(Component.translatable("frmc.additional_content_pack.button"), button -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof AccessibilityOptionsScreen parent) {
                minecraft.setScreen(new AdditionalContentPackScreen(parent));
            }
        }).bounds(x, y, width, 20).build();
    }
}
