package com.y271727uy.FRMC.mixin.embeddium.fade;

import com.y271727uy.FRMC.integration.embeddium.fade.ChunkFadeIn;
import com.y271727uy.FRMC.integration.embeddium.fade.FadeSectionManager;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 渲染器重载（F3+A、改视频设置）时把“已淡入”状态带到新的 RenderSectionManager。 */
@Mixin(value = SodiumWorldRenderer.class, remap = false)
public abstract class SodiumWorldRendererMixin {
    @Shadow private RenderSectionManager renderSectionManager;

    @Inject(method = "initRenderer", at = @At("HEAD"), require = 0)
    private void frmc$collectSettledSections(CommandList commandList, CallbackInfo ci) {
        ChunkFadeIn.settledSections = this.renderSectionManager != null && ChunkFadeIn.active()
            ? ((FadeSectionManager) this.renderSectionManager).frmc$collectSettledSections()
            : null;
    }

    @Inject(method = "initRenderer", at = @At("RETURN"), require = 0)
    private void frmc$releaseSettledSections(CommandList commandList, CallbackInfo ci) {
        ChunkFadeIn.settledSections = null;
    }
}
