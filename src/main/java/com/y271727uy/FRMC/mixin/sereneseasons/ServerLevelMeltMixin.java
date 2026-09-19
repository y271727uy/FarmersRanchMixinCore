package com.y271727uy.FRMC.mixin.sereneseasons;

import com.y271727uy.FRMC.integration.sereneseasons.SereneSeasonsMeltOptimizer;
import com.y271727uy.FRMC.integration.sereneseasons.SeasonSnapshotCache;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMeltMixin {
    @Inject(method = "tickChunk", at = @At("TAIL"), require = 1)
    private void frmc$runSereneSeasonsMelt(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        SereneSeasonsMeltOptimizer.tickChunk((ServerLevel) (Object) this, chunk);
    }

    @Inject(method = "unload", at = @At("TAIL"), require = 1)
    private void frmc$clearSeasonSnapshot(LevelChunk chunk, CallbackInfo ci) {
        SeasonSnapshotCache.invalidate((ServerLevel) (Object) this);
    }
}
