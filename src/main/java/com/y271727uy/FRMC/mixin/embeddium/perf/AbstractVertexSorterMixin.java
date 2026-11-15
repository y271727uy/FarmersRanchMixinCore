package com.y271727uy.FRMC.mixin.embeddium.perf;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.embeddium.EmbeddiumPortConfig;
import com.y271727uy.FRMC.integration.embeddium.perf.RadixSort;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 即时模式缓冲区（半透明实体、粒子等走原版 BufferBuilder 排序的图层）的面排序：面数多时换基数排序。
 * 0.3.31 里这次调用在私有的 mergeSort(Vector3f[]) 中，较新的 Embeddium 把它内联进了 sort，两处都列上。
 */
@Mixin(targets = "me.jellysquid.mods.sodium.client.util.sorting.VertexSorters$AbstractVertexSorter", remap = false)
public abstract class AbstractVertexSorterMixin {
    @WrapOperation(
        method = {"mergeSort", "sort", "m_277065_"},
        at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/util/sorting/MergeSort;mergeSort([F)[I"),
        require = 0
    )
    private int[] frmc$radixSortLargeBuffers(float[] keys, Operation<int[]> original) {
        if (keys.length >= RadixSort.THRESHOLD && EmbeddiumPortConfig.radixVertexSort()) {
            return RadixSort.sortDescending(keys);
        }
        return original.call(keys);
    }
}
