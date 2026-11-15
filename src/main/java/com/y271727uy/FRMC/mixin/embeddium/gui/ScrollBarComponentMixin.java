package com.y271727uy.FRMC.mixin.embeddium.gui;

import com.y271727uy.FRMC.integration.embeddium.gui.GuiAnimation;
import com.y271727uy.FRMC.integration.embeddium.gui.SmoothScrollBar;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import org.embeddedt.embeddium.gui.frame.components.ScrollBarComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 滚动条：滚轮/键盘滚动缓动到位，拖动时保持跟手；滑块悬停高亮。 */
@Mixin(value = ScrollBarComponent.class, remap = false)
public abstract class ScrollBarComponentMixin extends AbstractWidget implements SmoothScrollBar {
    @Shadow @Final protected Dim2i dim;
    @Shadow @Final private ScrollBarComponent.Mode mode;
    @Shadow @Final private int frameLength;
    @Shadow @Final private int viewPortLength;
    @Shadow @Final private int maxScrollBarOffset;
    @Shadow private int offset;
    @Shadow private boolean isDragging;
    @Shadow private Dim2i scrollThumb;

    @Unique private final GuiAnimation frmc$scrollEase = new GuiAnimation(45.0f, 0.0f);
    @Unique private final GuiAnimation frmc$thumbHighlight = new GuiAnimation(60.0f, 0.0f);
    @Unique private float frmc$highlight;

    @Override
    public float frmc$getRenderOffset() {
        if (!GuiAnimation.enabled()) {
            return this.offset;
        }
        this.frmc$scrollEase.setTarget(this.offset);
        return this.frmc$scrollEase.get();
    }

    @Inject(method = {"render", "m_88315_"}, at = @At("HEAD"), require = 0)
    private void frmc$easeThumb(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!GuiAnimation.enabled() || this.scrollThumb == null) {
            this.frmc$highlight = 0.0f;
            return;
        }

        boolean vertical = this.mode == ScrollBarComponent.Mode.VERTICAL;
        int thumbLength = (this.viewPortLength * (vertical ? this.dim.height() : this.dim.width() - 6)) / this.frameLength;
        int maxThumbOffset = this.viewPortLength - thumbLength;
        int thumbOffset = this.maxScrollBarOffset == 0
            ? 0
            : Math.round(this.frmc$getRenderOffset() * maxThumbOffset / (float) this.maxScrollBarOffset);

        this.scrollThumb = new Dim2i(
            this.dim.x() + 2 + (vertical ? 0 : thumbOffset),
            this.dim.y() + 2 + (vertical ? thumbOffset : 0),
            (vertical ? this.dim.width() : thumbLength) - 4,
            (vertical ? thumbLength : this.dim.height()) - 4);

        this.frmc$highlight = this.frmc$thumbHighlight.towards(this.isDragging || this.scrollThumb.containsCursor(mouseX, mouseY));
    }

    @ModifyArg(
        method = {"render", "m_88315_"},
        at = @At(value = "INVOKE", target = "drawRect(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V"),
        index = 5,
        require = 0
    )
    private int frmc$highlightThumb(int color) {
        return GuiAnimation.lerpColor(color, 0xFFFFFFFF, this.frmc$highlight);
    }

    // 拖动滑块时内容必须紧跟鼠标，不能再缓动。
    @Inject(method = "setOffset", at = @At("TAIL"), require = 0)
    private void frmc$snapWhileDragging(int value, CallbackInfo ci) {
        if (this.isDragging) {
            this.frmc$scrollEase.snapTo(this.offset);
        }
    }
}
