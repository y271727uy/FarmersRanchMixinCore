package com.y271727uy.FRMC.mixin.sereneseasons;

import com.y271727uy.FRMC.integration.sereneseasons.SeasonSnapshotCache;
import com.y271727uy.FRMC.integration.sereneseasons.SeasonSnapshotCache.Snapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import sereneseasons.init.ModTags;

@Pseudo
@Mixin(targets = "sereneseasons.season.SeasonHooks", remap = false)
public abstract class SeasonHooksMixin {
    @Inject(
        method = "getBiomeTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;)F",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 1
    )
    private static void frmc$useCachedSeason(
            Level level, Holder<Biome> biome, BlockPos pos,
            CallbackInfoReturnable<Float> cir) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Snapshot snapshot = SeasonSnapshotCache.get(serverLevel);
        if (!snapshot.dimensionWhitelisted() || biome.is(ModTags.Biomes.BLACKLISTED_BIOMES)) {
            cir.setReturnValue(biome.value().getBaseTemperature());
            return;
        }
        cir.setReturnValue(sereneseasons.season.SeasonHooks.getBiomeTemperatureInSeason(
            snapshot.subSeason(), biome, pos
        ));
    }
}
