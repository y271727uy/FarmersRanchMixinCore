package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.block.poi.ACPOIRegistry;
import com.github.alexmodguy.alexscaves.server.entity.util.MagnetUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MagnetUtil.class, remap = false)
public abstract class MagnetPoiQueryMixin {
    @WrapOperation(method = "tickMagnetism", at = @At(value = "INVOKE", target =
            "Lcom/github/alexmodguy/alexscaves/server/entity/util/MagnetUtil;getNearbyAttractingMagnets(Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;I)Ljava/util/stream/Stream;"),
            require = 1)
    private static Stream<BlockPos> frmc$findBothMagnetTypes(BlockPos center, ServerLevel level, int range,
            Operation<Stream<BlockPos>> original, @Share("repellingMagnets") LocalRef<List<BlockPos>> repellingMagnets) {
        var attractingKey = ACPOIRegistry.ATTRACTING_MAGNETS.getKey();
        var repellingKey = ACPOIRegistry.REPELLING_MAGNETS.getKey();
        List<BlockPos> attracting = null;
        List<BlockPos> repelling = null;
        try (var records = level.getPoiManager().getInRange(
                type -> type.is(attractingKey) || type.is(repellingKey), center, range, PoiManager.Occupancy.ANY)) {
            for (var iterator = records.iterator(); iterator.hasNext();) {
                var record = iterator.next();
                if (record.getPoiType().is(attractingKey)) {
                    if (attracting == null) {
                        attracting = new ArrayList<>();
                    }
                    attracting.add(record.getPos());
                } else {
                    if (repelling == null) {
                        repelling = new ArrayList<>();
                    }
                    repelling.add(record.getPos());
                }
            }
        }
        // The original force callbacks still run in their original order, with attraction first.
        repellingMagnets.set(repelling == null ? List.of() : repelling);
        return attracting == null ? Stream.empty() : attracting.stream();
    }

    @WrapOperation(method = "tickMagnetism", at = @At(value = "INVOKE", target =
            "Lcom/github/alexmodguy/alexscaves/server/entity/util/MagnetUtil;getNearbyRepellingMagnets(Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;I)Ljava/util/stream/Stream;"),
            require = 1)
    private static Stream<BlockPos> frmc$reuseRepellingMagnets(BlockPos center, ServerLevel level, int range,
            Operation<Stream<BlockPos>> original, @Share("repellingMagnets") LocalRef<List<BlockPos>> repellingMagnets) {
        List<BlockPos> repelling = repellingMagnets.get();
        return repelling == null ? original.call(center, level, range) : repelling.stream();
    }
}
