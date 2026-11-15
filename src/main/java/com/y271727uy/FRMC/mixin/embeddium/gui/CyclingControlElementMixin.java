package com.y271727uy.FRMC.mixin.embeddium.gui;

import com.y271727uy.FRMC.integration.embeddium.gui.GuiAnimation;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 循环选项：值变化时，新文字从右侧 4 像素处滑入并淡入。 */
@Mixin(targets = "me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl$CyclingControlElement", remap = false)
public abstract class CyclingControlElementMixin<T extends Enum<T>> extends ControlElement<T> {
    @Unique private final GuiAnimation frmc$valueSettle = new GuiAnimation(60.0f, 1.0f);
    @Unique private int frmc$lastDrawnOrdinal = -1;

    private CyclingControlElementMixin(Option<T> option, Dim2i dim) {
        super(option, dim);
    }

    @Unique
    private float frmc$settle() {
        int ordinal = this.option.getValue().ordinal();
        if (this.frmc$lastDrawnOrdinal != -1 && this.frmc$lastDrawnOrdinal != ordinal) {
            this.frmc$valueSettle.setTarget(1.0f);
            this.frmc$valueSettle.restartFrom(0.0f);
        }
        this.frmc$lastDrawnOrdinal = ordinal;
        return GuiAnimation.smoothstep(this.frmc$valueSettle.get());
    }

    @ModifyArg(
        method = {"render", "m_88315_"},
        at = @At(value = "INVOKE", target = "drawString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/network/chat/Component;III)V"),
        index = 2,
        require = 0
    )
    private int frmc$slideValue(int x) {
        return GuiAnimation.enabled() ? x + Math.round(4.0f * (1.0f - this.frmc$settle())) : x;
    }

    @ModifyArg(
        method = {"render", "m_88315_"},
        at = @At(value = "INVOKE", target = "drawString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/network/chat/Component;III)V"),
        index = 4,
        require = 0
    )
    private int frmc$fadeValue(int color) {
        return GuiAnimation.enabled() ? GuiAnimation.scaleAlpha(color, 0.35f + 0.65f * this.frmc$settle()) : color;
    }
}
