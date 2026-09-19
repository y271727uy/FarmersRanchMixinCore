package com.y271727uy.FRMC.integration.xaero;

import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.client.gui.MapIconButton;
import com.y271727uy.FRMC.capability.blockchecking.client.ClientBlockCheckingNetworkHandler;
import com.y271727uy.FRMC.network.BlockCheckingNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import xaero.map.gui.GuiMap;

/** Client-only boundary for the optional Xaero World Map module. */
public final class XaeroWorldMapIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final XaeroIntegrationState STATE = new XaeroIntegrationState();
    private static boolean overlayEnabled = false;
    private static MapIconButton overlayButton;

    private XaeroWorldMapIntegration() {
    }

    public static void initialize(boolean available) {
        LOGGER.info("XaeroWorldMapIntegration.initialize() called with available={}", available);
        STATE.initialize(available);
    }

    public static XaeroIntegrationState state() {
        return STATE;
    }

    public static void initButton(GuiMap map) {
        LOGGER.info("XaeroWorldMapIntegration.initButton() called - creating performance button");
        try {
            overlayButton = new MapIconButton(
                3,
                20,
                Component.translatable("frmc.blockchecking.map.button"),
                button -> {
                    overlayEnabled = !overlayEnabled;
                    if (overlayEnabled) {
                        ClientBlockCheckingNetworkHandler.STATE.invalidateSnapshot();
                        BlockCheckingNetwork.requestAnalysis(true);
                    } else {
                        BlockCheckingNetwork.requestAnalysis(false);
                    }
                    if (button instanceof MapIconButton iconButton) {
                        iconButton.setActive(overlayEnabled);
                    }
                }
            );
            overlayButton.setActive(overlayEnabled);
            map.addButton(overlayButton);
            LOGGER.info("XaeroWorldMapIntegration button added successfully");
        } catch (Throwable ex) {
            LOGGER.error("Failed to add XaeroWorldMapIntegration button", ex);
        }
    }

    public static void afterRender(GuiMap map, GuiGraphics graphics, int mouseX, int mouseY,
            double cameraX, double cameraZ, double mapScale) {
        if (!overlayEnabled) {
            return;
        }
        try {
            XaeroWorldMapOverlay.render(graphics, map, mouseX, mouseY, cameraX, cameraZ, mapScale);
        } catch (Throwable ignored) {
            // Overlay is best-effort; do not fuse the whole Xaero integration.
        }
    }
    
    public static boolean isOverlayEnabled() {
        return overlayEnabled;
    }
    
    public static void setOverlayEnabled(boolean enabled) {
        overlayEnabled = enabled;
        if (overlayButton != null) {
            overlayButton.setActive(enabled);
        }
    }

    public static void inspectChunk(int blockX, int blockZ, ResourceKey<Level> dimension) {
        Minecraft minecraft = Minecraft.getInstance();
        String dimId;
        if (dimension != null) {
            dimId = dimension.location().toString();
        } else if (minecraft.level != null) {
            dimId = minecraft.level.dimension().location().toString();
        } else {
            return;
        }
        setOverlayEnabled(true);
        BlockCheckingNetwork.requestInspectChunk(dimId, ChunkPos.asLong(Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16)));
    }

    public static void uninspectChunk(int blockX, int blockZ, ResourceKey<Level> dimension) {
        Minecraft minecraft = Minecraft.getInstance();
        String dimId;
        if (dimension != null) {
            dimId = dimension.location().toString();
        } else if (minecraft.level != null) {
            dimId = minecraft.level.dimension().location().toString();
        } else {
            return;
        }
        setOverlayEnabled(true);
        BlockCheckingNetwork.requestUninspectChunk(dimId, ChunkPos.asLong(Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16)));
    }
}
