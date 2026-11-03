package com.y271727uy.FRMC.mixin.acceleratedrendering;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.github.alexthe666.citadel.client.model.basic.BasicModelPart$PositionTextureVertex", remap = false)
public interface PositionTextureVertexAccessorMixin {
    @Accessor("position")
    Vector3f frmc$getPosition();

    @Accessor("textureU")
    float frmc$getTextureU();

    @Accessor("textureV")
    float frmc$getTextureV();
}
