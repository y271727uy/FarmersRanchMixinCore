package com.y271727uy.FRMC.mixin.minecraft.vertex;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessorMixin {
    @Accessor("format")
    VertexFormat frmc$getFormat();
}
