package com.y271727uy.FRMC.mixin.minecraft.worldgen.diagnostics;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.y271727uy.FRMC.capability.worldgen.BiomeLookupDiagnostics;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;

/** 诊断专用：只在 {@code biomeLookupDiagnostics} 打开时织入，只计数和计时，不改变返回值。 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceLookupMixin {
    @WrapMethod(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;")
    private Holder<Biome> frmc$measureLookup(int quartX, int quartY, int quartZ, Climate.Sampler sampler,
                                             Operation<Holder<Biome>> original) {
        long start = BiomeLookupDiagnostics.beforeLookup(this, quartX, quartY, quartZ);
        try {
            return original.call(quartX, quartY, quartZ, sampler);
        } finally {
            BiomeLookupDiagnostics.afterLookup(start);
        }
    }
}
