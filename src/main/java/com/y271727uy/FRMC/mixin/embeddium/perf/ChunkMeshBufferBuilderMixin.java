package com.y271727uy.FRMC.mixin.embeddium.perf;

import com.y271727uy.FRMC.integration.embeddium.EmbeddiumPortConfig;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 修正网格缓冲区扩容的单位错误。grow 和 setBufferSize 的参数都是“顶点数”，setBufferSize 内部自己会乘步长，
 * 但 Embeddium 0.3.31 在两处调用前各多乘了一次步长，一次扩容就把容量放大数十倍。
 */
@Mixin(value = ChunkMeshBufferBuilder.class, remap = false)
public abstract class ChunkMeshBufferBuilderMixin {
    @Shadow @Final private int stride;

    @ModifyArg(method = "push", at = @At(value = "INVOKE", target = "grow(I)V"), require = 0)
    private int frmc$growByVertices(int strideTimesVertexCount) {
        return EmbeddiumPortConfig.meshBufferGrowthFix() ? strideTimesVertexCount / this.stride : strideTimesVertexCount;
    }

    @ModifyArg(method = "grow", at = @At(value = "INVOKE", target = "setBufferSize(I)V"), require = 0)
    private int frmc$resizeByVertices(int capacityTimesStride) {
        return EmbeddiumPortConfig.meshBufferGrowthFix() ? capacityTimesStride / this.stride : capacityTimesStride;
    }
}
