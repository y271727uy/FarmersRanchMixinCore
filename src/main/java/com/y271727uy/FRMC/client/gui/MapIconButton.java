package com.y271727uy.FRMC.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 一个方形图标按钮，模仿 Xaero 地图右侧按钮的风格
 */
public class MapIconButton extends Button {
    private static final int SIZE = 32; // 按钮大小改为32，与图标一致
    
    private static final ResourceLocation ICON_OPEN = new ResourceLocation("frmc", "textures/gui/open.png");
    private static final ResourceLocation ICON_CLOSE = new ResourceLocation("frmc", "textures/gui/close.png");
    
    private boolean active;
    
    public MapIconButton(int x, int y, Component message, OnPress onPress) {
        super(x, y, SIZE, SIZE, message, onPress, DEFAULT_NARRATION);
        this.active = false;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public boolean isActive() {
        return active;
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }

        ResourceLocation icon = active ? ICON_OPEN : ICON_CLOSE;

        // 参数：resourceLocation, x, y, uOffset(float), vOffset(float), width, height, textureWidth, textureHeight
        graphics.blit(icon, this.getX(), this.getY(), 0.0F, 0.0F, 32, 32, 32, 32);
    }
}
