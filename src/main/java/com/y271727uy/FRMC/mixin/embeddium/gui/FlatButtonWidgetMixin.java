package com.y271727uy.FRMC.mixin.embeddium.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.embeddium.gui.GuiAnimation;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import org.embeddedt.embeddium.gui.theme.DefaultColors;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 按钮与标签页条目：悬停/禁用渐变，选中标记从中心向两端展开。 */
@Mixin(value = FlatButtonWidget.class, remap = false)
public abstract class FlatButtonWidgetMixin extends AbstractWidget {
    @Shadow @Final protected Dim2i dim;
    @Shadow private FlatButtonWidget.Style style;
    @Shadow private boolean selected;
    @Shadow private boolean enabled;
    @Shadow private boolean visible;
    @Shadow private boolean leftAligned;

    @Unique private final GuiAnimation frmc$hoverFade = new GuiAnimation(55.0f, 0.0f);
    @Unique private final GuiAnimation frmc$selectGrow = new GuiAnimation(70.0f, 0.0f);
    @Unique private final GuiAnimation frmc$enableFade = new GuiAnimation(80.0f, 1.0f);
    @Unique private float frmc$enabledFade = 1.0f;

    @ModifyArg(
        method = {"render", "m_88315_"},
        at = @At(value = "INVOKE", target = "drawRect(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V", ordinal = 0),
        index = 5,
        require = 0
    )
    private int frmc$fadeBackground(int color) {
        if (!GuiAnimation.enabled()) {
            return color;
        }
        float hover = this.frmc$hoverFade.towards(this.enabled && this.hovered);
        this.frmc$enabledFade = this.frmc$enableFade.towards(this.enabled);
        return GuiAnimation.lerpColor(this.style.bgDisabled,
            GuiAnimation.lerpColor(this.style.bgDefault, this.style.bgHovered, hover), this.frmc$enabledFade);
    }

    @ModifyArg(
        method = {"render", "m_88315_"},
        at = @At(value = "INVOKE", target = "drawString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/network/chat/Component;III)V"),
        index = 4,
        require = 0
    )
    private int frmc$fadeText(int color) {
        if (!GuiAnimation.enabled()) {
            return color;
        }
        return GuiAnimation.lerpColor(this.style.textDisabled, this.style.textDefault, this.frmc$enabledFade);
    }

    // 原版只在选中时画标记；动画开启时改由下面的 TAIL 注入统一绘制，这样取消选中也能播放收回动画。
    @WrapOperation(
        method = {"render", "m_88315_"},
        at = @At(value = "INVOKE", target = "drawRect(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V", ordinal = 1),
        require = 0
    )
    private void frmc$skipStaticMarker(FlatButtonWidget self, GuiGraphics graphics, int x1, int y1, int x2, int y2, int color,
                                       Operation<Void> original) {
        if (!GuiAnimation.enabled()) {
            original.call(self, graphics, x1, y1, x2, y2, color);
        }
    }

    @Inject(method = {"render", "m_88315_"}, at = @At("TAIL"), require = 0)
    private void frmc$renderAnimatedMarker(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!GuiAnimation.enabled() || !this.visible) {
            return;
        }

        float selectedFade = this.frmc$selectGrow.towards(this.enabled && this.selected);
        if (selectedFade <= 0.0f) {
            return;
        }

        int markerColor = GuiAnimation.scaleAlpha(DefaultColors.ELEMENT_ACTIVATED, Math.min(1.0f, selectedFade * 2.0f));
        float grow = GuiAnimation.smoothstep(selectedFade);

        if (this.leftAligned) {
            int half = Math.round((this.dim.height() / 2.0f) * grow);
            int center = this.dim.getCenterY();
            this.drawRect(graphics, this.dim.x(), center - half, this.dim.x() + 1, center + half, markerColor);
        } else {
            int half = Math.round((this.dim.width() / 2.0f) * grow);
            int center = this.dim.getCenterX();
            this.drawRect(graphics, center - half, this.dim.getLimitY() - 1, center + half, this.dim.getLimitY(), markerColor);
        }
    }
}
