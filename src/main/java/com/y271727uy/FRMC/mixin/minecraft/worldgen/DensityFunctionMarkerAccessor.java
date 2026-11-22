package com.y271727uy.FRMC.mixin.minecraft.worldgen;

import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** {@code DensityFunctions.Marker} 在 DensityFunctions 里是 protected，外部包只能通过这个 accessor 取被包装的子树。 */
@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$Marker")
public interface DensityFunctionMarkerAccessor {
    @Accessor("wrapped")
    DensityFunction frmc$wrapped();
}
