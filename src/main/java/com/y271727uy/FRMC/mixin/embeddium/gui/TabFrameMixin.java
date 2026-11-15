package com.y271727uy.FRMC.mixin.embeddium.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.embeddium.gui.GuiAnimation;
import net.minecraft.client.gui.GuiGraphics;
import org.embeddedt.embeddium.gui.frame.AbstractFrame;
import org.embeddedt.embeddium.gui.frame.tab.Tab;
import org.embeddedt.embeddium.gui.frame.tab.TabFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 标签页：切换时右侧内容从 10 像素外滑入。 */
@Mixin(value = TabFrame.class, remap = false)
public abstract class TabFrameMixin {
    @Unique private static final float FRMC_TAB_SLIDE_DISTANCE = 10.0f;

    @Shadow private Tab<?> selectedTab;

    @Unique private final GuiAnimation frmc$tabSlide = new GuiAnimation(55.0f, 0.0f);

    @Inject(method = "setTab", at = @At("HEAD"), require = 0)
    private void frmc$startSlide(Tab<?> tab, CallbackInfo ci) {
        if (GuiAnimation.enabled() && this.selectedTab != tab) {
            this.frmc$tabSlide.setTarget(0.0f);
            this.frmc$tabSlide.restartFrom(FRMC_TAB_SLIDE_DISTANCE);
        }
    }

    // 只包住选中页面的绘制；侧边栏等其它子控件走的是 AbstractWidget.render，不受影响。
    @WrapOperation(
        method = {"render", "m_88315_"},
        at = {
            @At(value = "INVOKE", target = "Lorg/embeddedt/embeddium/gui/frame/AbstractFrame;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"),
            @At(value = "INVOKE", target = "Lorg/embeddedt/embeddium/gui/frame/AbstractFrame;m_88315_(Lnet/minecraft/client/gui/GuiGraphics;IIF)V")
        },
        require = 0
    )
    private void frmc$slideSelectedFrame(AbstractFrame frame, GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                         Operation<Void> original) {
        int shift = GuiAnimation.enabled() ? Math.round(this.frmc$tabSlide.get()) : 0;
        if (shift == 0) {
            original.call(frame, graphics, mouseX, mouseY, delta);
            return;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(shift, 0.0f, 0.0f);
        try {
            original.call(frame, graphics, mouseX - shift, mouseY, delta);
        } finally {
            graphics.pose().popPose();
        }
    }
}
