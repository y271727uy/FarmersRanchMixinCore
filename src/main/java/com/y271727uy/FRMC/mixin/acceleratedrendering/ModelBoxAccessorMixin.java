package com.y271727uy.FRMC.mixin.acceleratedrendering;

import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.github.alexthe666.citadel.client.model.basic.BasicModelPart$ModelBox", remap = false)
public interface ModelBoxAccessorMixin {
    @Accessor("quads")
    Object[] frmc$getQuads();
}
