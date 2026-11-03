package com.y271727uy.FRMC.mixin.acceleratedrendering;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.github.alexthe666.citadel.client.model.basic.BasicModelPart$TexturedQuad", remap = false)
public interface TexturedQuadAccessorMixin {
    @Accessor("vertexPositions")
    Object[] frmc$getVertexPositions();

    @Accessor("normal")
    Vector3f frmc$getNormal();
}
