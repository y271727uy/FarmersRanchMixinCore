package com.y271727uy.FRMC.client.mcmod;

import com.mojang.blaze3d.platform.InputConstants;
import com.y271727uy.FRMC.FRMCMod;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Opens a browser search for the item in the hovered container slot. */
@Mod.EventBusSubscriber(modid = FRMCMod.MODID, value = Dist.CLIENT)
public final class McmodSearchClientEvents {
    private static final KeyMapping SEARCH_KEY = new KeyMapping(
            "key.frmc.search_mcmod", KeyConflictContext.GUI, KeyModifier.NONE,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_HOME, "key.categories.frmc");
    private static boolean searchKeyHeld;

    private McmodSearchClientEvents() {
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || screen.getFocused() instanceof EditBox
                || !SEARCH_KEY.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) {
            return;
        }
        if (searchKeyHeld) {
            event.setCanceled(true);
            return;
        }
        Slot slot = screen.getSlotUnderMouse();
        if (slot == null || !slot.hasItem()) {
            return;
        }
        String name = slot.getItem().getHoverName().getString();
        if (name.isBlank()) {
            return;
        }
        searchKeyHeld = true;
        event.setCanceled(true);
        String query = URLEncoder.encode(name, StandardCharsets.UTF_8);
        Util.getPlatform().openUri("https://search.mcmod.cn/s?key=" + query);
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onKeyReleased(ScreenEvent.KeyReleased.Pre event) {
        if (SEARCH_KEY.matches(event.getKeyCode(), event.getScanCode())) {
            searchKeyHeld = false;
        }
    }

    @SubscribeEvent
    public static void onScreenClosed(ScreenEvent.Closing event) {
        searchKeyHeld = false;
    }

    @Mod.EventBusSubscriber(modid = FRMCMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class KeyRegistration {
        private KeyRegistration() {
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(SEARCH_KEY);
        }
    }
}
