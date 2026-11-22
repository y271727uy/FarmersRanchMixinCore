package com.y271727uy.FRMC.mixin.minecraft.worldgen.diagnostics;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.y271727uy.FRMC.capability.worldgen.BiomeLookupDiagnostics;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;

/** 诊断专用：标记"区块正在填充自己的群系"，让查询能按调用场景分类。 */
@Mixin(ChunkAccess.class)
public abstract class ChunkAccessBiomeFillMixin {
    @WrapMethod(method = "fillBiomesFromNoise")
    private void frmc$markBiomeFill(BiomeResolver resolver, Climate.Sampler sampler, Operation<Void> original) {
        BiomeLookupDiagnostics.enterChunkFill();
        try {
            original.call(resolver, sampler);
        } finally {
            BiomeLookupDiagnostics.exitChunkFill();
        }
    }
}
