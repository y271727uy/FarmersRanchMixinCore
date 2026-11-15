package com.y271727uy.FRMC.mixin.embeddium.perf;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.embeddium.perf.VisibleSpriteTicker;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** 接管后跳过 setupTerrain 里每帧一次的标记，改由 MinecraftSpriteTickMixin 在每 tick 开头触发。 */
@Mixin(value = SodiumWorldRenderer.class, remap = false)
public abstract class SodiumWorldRendererSpriteTickMixin implements VisibleSpriteTicker {
    @Shadow
    private RenderSectionManager renderSectionManager;

    @WrapOperation(
        method = "setupTerrain",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/chunk/RenderSectionManager;tickVisibleRenders()V"
        ),
        require = 0
    )
    private void frmc$skipPerFrameMarking(RenderSectionManager manager, Operation<Void> original) {
        if (!VisibleSpriteTicker.perTick()) {
            original.call(manager);
        }
    }

    @Override
    public void frmc$markVisibleSprites() {
        RenderSectionManager manager = this.renderSectionManager;
        if (manager != null) {
            manager.tickVisibleRenders();
        }
    }
}
