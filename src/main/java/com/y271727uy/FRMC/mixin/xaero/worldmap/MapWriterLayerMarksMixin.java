package com.y271727uy.FRMC.mixin.xaero.worldmap;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.integration.xaero.XaeroPerformanceConfig;
import java.util.function.BiConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapWriter;
import xaero.map.region.LayeredRegionManager;
import xaero.map.region.MapLayer;

@Mixin(value = MapWriter.class, remap = false)
public abstract class MapWriterLayerMarksMixin {
    @Unique private LayeredRegionManager frmc$markedManager;
    @Unique private int frmc$markedLayer;
    @Unique private int frmc$markedRegionX;
    @Unique private int frmc$markedRegionZ;

    @Inject(method = "onRender", at = @At("HEAD"), require = 1)
    private void frmc$beginWriterPass(CallbackInfo ci) {
        frmc$markedManager = null;
    }

    @WrapOperation(method = "writeChunk", at = @At(value = "INVOKE", target =
            "Lxaero/map/region/LayeredRegionManager;applyToEachLoadedLayer(Ljava/util/function/BiConsumer;)V"),
            require = 1, expect = 1, allow = 1)
    private void frmc$markRegionOnce(LayeredRegionManager manager, BiConsumer<Integer, MapLayer> marker,
            Operation<Void> original, @Local(argsOnly = true, ordinal = 3) int layer,
            @Local(argsOnly = true, ordinal = 4) int tileChunkX,
            @Local(argsOnly = true, ordinal = 5) int tileChunkZ) {
        if (!XaeroPerformanceConfig.deduplicateLayerMarks()) {
            original.call(manager, marker);
            return;
        }
        int regionX = tileChunkX >> 3;
        int regionZ = tileChunkZ >> 3;
        if (manager == frmc$markedManager && layer == frmc$markedLayer
                && regionX == frmc$markedRegionX && regionZ == frmc$markedRegionZ) {
            return;
        }
        original.call(manager, marker);
        frmc$markedManager = manager;
        frmc$markedLayer = layer;
        frmc$markedRegionX = regionX;
        frmc$markedRegionZ = regionZ;
    }
}
