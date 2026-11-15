package com.y271727uy.FRMC.mixin.embeddium.fade;

import com.y271727uy.FRMC.integration.embeddium.fade.FadeRegion;
import com.y271727uy.FRMC.integration.embeddium.fade.FadeSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionFlags;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSection.class, remap = false)
public abstract class RenderSectionMixin implements FadeSection {
    @Unique private boolean frmc$fadeInDecided;

    @Shadow public abstract int getSectionIndex();

    @Shadow public abstract RenderRegion getRegion();

    @Override
    public void frmc$skipFadeIn() {
        this.frmc$fadeInDecided = true;
    }

    @Override
    public boolean frmc$isFadeInDecided() {
        return this.frmc$fadeInDecided;
    }

    // 只有第一次拿到构建结果时才淡入；之后的方块更新重建不再触发。
    @Inject(method = "setRenderState", at = @At("HEAD"), require = 0)
    private void frmc$startFadeIn(BuiltSectionInfo info, CallbackInfo ci) {
        if (this.frmc$fadeInDecided) {
            return;
        }
        this.frmc$fadeInDecided = true;

        if ((info.flags & (1 << RenderSectionFlags.HAS_BLOCK_GEOMETRY)) != 0) {
            ((FadeRegion) (Object) this.getRegion()).frmc$startFadeIn(this.getSectionIndex());
        }
    }
}
