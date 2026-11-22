package com.y271727uy.FRMC.mixin.minecraft.worldgen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** {@code DensityFunctions.Noise} 是 protected；气候记忆的 y 无关性检查需要它的 yScale。 */
@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$Noise")
public interface DensityFunctionNoiseAccessor {
    @Accessor("yScale")
    double frmc$yScale();
}
