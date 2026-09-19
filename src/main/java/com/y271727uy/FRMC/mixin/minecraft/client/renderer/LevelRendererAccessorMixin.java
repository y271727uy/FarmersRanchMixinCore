package com.y271727uy.FRMC.mixin.minecraft.client.renderer;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessorMixin {
    @Accessor("cullingFrustum")
    Frustum frmc$getCullingFrustum();
}
