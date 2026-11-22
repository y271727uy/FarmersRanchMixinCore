package com.y271727uy.FRMC.mixin.minecraft.worldgen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** {@code DensityFunctions.ShiftedNoise} 是 protected；气候记忆的 y 无关性检查需要它的 yScale（shiftY 作为子节点单独检查）。 */
@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$ShiftedNoise")
public interface DensityFunctionShiftedNoiseAccessor {
    @Accessor("yScale")
    double frmc$yScale();
}
