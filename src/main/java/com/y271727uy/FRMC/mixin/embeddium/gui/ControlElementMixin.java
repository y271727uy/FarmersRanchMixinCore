package com.y271727uy.FRMC.mixin.embeddium.gui;

import com.y271727uy.FRMC.integration.embeddium.gui.GuiAnimation;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 选项行：悬停背景渐变，标题随悬停右移 2 像素。 */
@Mixin(value = ControlElement.class, remap = false)
public abstract class ControlElementMixin extends AbstractWidget {
    @Shadow private FlatButtonWidget.Style style;

    @Unique private final GuiAnimation frmc$hoverFade = new GuiAnimation(55.0f, 0.0f);
    @Unique private float frmc$hover;

    @ModifyArg(
        method = {"render", "m_88315_"},
        at = @At(value = "INVOKE", target = "drawRect(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V"),
        index = 5,
        require = 0
    )
    private int frmc$fadeBackground(int color) {
        if (!GuiAnimation.enabled()) {
            this.frmc$hover = 0.0f;
            return color;
        }
        this.frmc$hover = this.frmc$hoverFade.towards(this.hovered);
        return GuiAnimation.lerpColor(this.style.bgDefault, this.style.bgHovered, this.frmc$hover);
    }

    @ModifyArg(
        method = {"render", "m_88315_"},
        at = @At(value = "INVOKE", target = "drawString(Lnet/minecraft/client/gui/GuiGraphics;Ljava/lang/String;III)V"),
        index = 2,
        require = 0
    )
    private int frmc$nudgeLabel(int x) {
        return x + Math.round(2.0f * GuiAnimation.smoothstep(this.frmc$hover));
    }
}
