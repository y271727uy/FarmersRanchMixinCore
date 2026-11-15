package com.y271727uy.FRMC.mixin.embeddium.gui;

import com.y271727uy.FRMC.integration.embeddium.gui.GuiAnimation;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 滑条：悬停时数值与滑轨交叉淡入淡出，滑轨从滑块位置向两侧展开。 */
@Mixin(targets = "me.jellysquid.mods.sodium.client.gui.options.control.SliderControl$Button", remap = false)
public abstract class SliderControlButtonMixin extends ControlElement<Integer> {
    @Unique private static final int FRMC_THUMB_WIDTH = 2;
    @Unique private static final int FRMC_TRACK_HEIGHT = 1;

    @Shadow @Final private Rect2i sliderBounds;
    @Shadow @Final private ControlValueFormatter formatter;
    @Shadow @Final private int min;
    @Shadow @Final private int range;
    @Shadow private double thumbPosition;

    @Shadow public abstract int getIntValue();

    @Shadow public abstract double getThumbPositionForValue(int value);

    @Unique private final GuiAnimation frmc$reveal = new GuiAnimation(60.0f, 0.0f);

    private SliderControlButtonMixin(Option<Integer> option, Dim2i dim) {
        super(option, dim);
    }

    // super.render 已经画完行背景；从这里接管数值和滑轨的绘制。
    @Inject(
        method = {"render", "m_88315_"},
        at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/gui/options/Option;isAvailable()Z"),
        cancellable = true,
        require = 0
    )
    private void frmc$renderAnimatedSlider(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!GuiAnimation.enabled()) {
            return;
        }
        ci.cancel();

        float shown = this.frmc$reveal.towards(this.option.isAvailable() && (this.hovered || this.isFocused()));

        if (GuiAnimation.isTextVisible(1.0f - shown)) {
            this.frmc$renderStandaloneValue(graphics, 1.0f - shown);
        }
        if (GuiAnimation.isTextVisible(shown)) {
            this.frmc$renderSlider(graphics, shown);
        }
    }

    @Unique
    private void frmc$renderStandaloneValue(GuiGraphics graphics, float alpha) {
        int sliderX = this.sliderBounds.getX();
        int sliderY = this.sliderBounds.getY();
        int sliderWidth = this.sliderBounds.getWidth();
        int sliderHeight = this.sliderBounds.getHeight();

        Component label = this.formatter.format(this.option.getValue());
        int labelWidth = this.font.width(label);

        this.drawString(graphics, label, sliderX + sliderWidth - labelWidth, sliderY + (sliderHeight / 2) - 4,
            GuiAnimation.scaleAlpha(0xFFFFFFFF, alpha));
    }

    @Unique
    private void frmc$renderSlider(GuiGraphics graphics, float alpha) {
        int sliderX = this.sliderBounds.getX();
        int sliderY = this.sliderBounds.getY();
        int sliderWidth = this.sliderBounds.getWidth();
        int sliderHeight = this.sliderBounds.getHeight();

        this.thumbPosition = this.getThumbPositionForValue(this.option.getValue());

        double thumbOffset = Mth.clamp((double) (this.getIntValue() - this.min) / this.range * sliderWidth, 0, sliderWidth);

        int thumbX = (int) (sliderX + thumbOffset - FRMC_THUMB_WIDTH);
        int trackY = (int) (sliderY + (sliderHeight / 2f) - ((double) FRMC_TRACK_HEIGHT / 2));

        int color = GuiAnimation.scaleAlpha(0xFFFFFFFF, alpha);

        float wipe = GuiAnimation.smoothstep(alpha);
        int thumbCenter = thumbX + FRMC_THUMB_WIDTH;
        int trackLeft = Math.round(thumbCenter - (thumbCenter - sliderX) * wipe);
        int trackRight = Math.round(thumbCenter + ((sliderX + sliderWidth) - thumbCenter) * wipe);

        this.drawRect(graphics, thumbX, sliderY, thumbX + (FRMC_THUMB_WIDTH * 2), sliderY + sliderHeight, color);
        this.drawRect(graphics, trackLeft, trackY, trackRight, trackY + FRMC_TRACK_HEIGHT, color);

        String label = this.formatter.format(this.getIntValue()).getString();
        int labelWidth = this.font.width(label);

        this.drawString(graphics, label, sliderX - labelWidth - 6, sliderY + (sliderHeight / 2) - 4, color);
    }
}
