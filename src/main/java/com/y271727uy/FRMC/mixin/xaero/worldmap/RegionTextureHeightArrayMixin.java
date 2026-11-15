package com.y271727uy.FRMC.mixin.xaero.worldmap;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.xaero.LazyHeightArray;
import com.y271727uy.FRMC.integration.xaero.XaeroPerformanceConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.misc.ConsistentBitArray;
import xaero.map.region.texture.RegionTexture;

@Mixin(value = RegionTexture.class, remap = false)
public abstract class RegionTextureHeightArrayMixin {
    @WrapOperation(method = "<init>", at = @At(value = "NEW",
            target = "(II)Lxaero/map/misc/ConsistentBitArray;"), require = 2, expect = 2, allow = 2)
    private ConsistentBitArray frmc$lazyHeightArray(int bits, int entries, Operation<ConsistentBitArray> original) {
        if (bits == 13 && entries == 4096 && XaeroPerformanceConfig.lazyHeights()) {
            return new LazyHeightArray();
        }
        return original.call(bits, entries);
    }
}
