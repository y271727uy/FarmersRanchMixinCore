package com.y271727uy.FRMC.mixin.embeddium.gui;

import com.y271727uy.FRMC.integration.embeddium.gui.GuiAnimation;
import com.y271727uy.FRMC.integration.embeddium.gui.SmoothScrollBar;
import org.embeddedt.embeddium.gui.frame.ScrollableFrame;
import org.embeddedt.embeddium.gui.frame.components.ScrollBarComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 可滚动面板：内容平移和鼠标坐标换算改用缓动后的偏移，逻辑偏移（焦点吸附等）不变。 */
@Mixin(value = ScrollableFrame.class, remap = false)
public abstract class ScrollableFrameMixin {
    @Inject(method = "applyOffset", at = @At("HEAD"), cancellable = true, require = 0)
    private void frmc$applySmoothOffset(ScrollBarComponent component, double coord, boolean negate,
                                        CallbackInfoReturnable<Double> cir) {
        if (component != null && GuiAnimation.enabled()) {
            float offset = ((SmoothScrollBar) (Object) component).frmc$getRenderOffset();
            cir.setReturnValue(coord + (offset * (negate ? -1 : 1)));
        }
    }
}
