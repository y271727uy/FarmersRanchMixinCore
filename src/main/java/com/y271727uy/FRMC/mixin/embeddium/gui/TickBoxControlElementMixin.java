package com.y271727uy.FRMC.mixin.embeddium.gui;

import com.y271727uy.FRMC.integration.embeddium.gui.GuiAnimation;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import org.embeddedt.embeddium.gui.theme.DefaultColors;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 勾选框：填充块从中心长出并淡入，取消勾选时反向播放。 */
@Mixin(targets = "me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl$TickBoxControlElement", remap = false)
public abstract class TickBoxControlElementMixin extends ControlElement<Boolean> {
    @Shadow @Final private Rect2i button;

    @Unique private final GuiAnimation frmc$tick = new GuiAnimation(50.0f, 0.0f);

    private TickBoxControlElementMixin(Option<Boolean> option, Dim2i dim) {
        super(option, dim);
    }

    // super.render 已经画完行背景；从这里接管勾选框本身的绘制。
    @Inject(
        method = {"render", "m_88315_"},
        at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/gui/options/Option;isAvailable()Z"),
        cancellable = true,
        require = 0
    )
    private void frmc$renderAnimatedTickBox(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!GuiAnimation.enabled()) {
            return;
        }
        ci.cancel();

        int x = this.button.getX();
        int y = this.button.getY();
        int w = x + this.button.getWidth();
        int h = y + this.button.getHeight();

        boolean enabled = this.option.isAvailable();
        float filled = this.frmc$tick.towards(this.option.getValue());
        int color = enabled
            ? GuiAnimation.lerpColor(0xFFFFFFFF, DefaultColors.ELEMENT_ACTIVATED, filled)
            : 0xFFAAAAAA;

        if (filled > 0.0f) {
            float grow = GuiAnimation.smoothstep(filled);
            float centerX = (x + w) / 2.0f;
            float centerY = (y + h) / 2.0f;
            float halfW = ((w - 2) - (x + 2)) / 2.0f * grow;
            float halfH = ((h - 2) - (y + 2)) / 2.0f * grow;
            int fillColor = enabled ? GuiAnimation.scaleAlpha(color, Math.min(1.0f, filled * 1.5f)) : color;

            this.drawRect(graphics,
                Math.round(centerX - halfW), Math.round(centerY - halfH),
                Math.round(centerX + halfW), Math.round(centerY + halfH),
                fillColor);
        }

        this.drawBorder(graphics, x, y, w, h, color);
    }
}
