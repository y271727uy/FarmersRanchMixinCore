package com.y271727uy.FRMC.mixin.starlight;

import ca.spottedleaf.starlight.common.chunk.ExtendedChunk;
import ca.spottedleaf.starlight.common.light.StarLightEngine;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.virtualWorld.VirtualChunk")
public abstract class VirtualChunkMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void frmc$initializeStarlightData(CallbackInfo ci) {
        ExtendedChunk chunk = (ExtendedChunk) (ChunkAccess) (Object) this;
        Object world = ((com.simibubi.create.foundation.virtualWorld.VirtualChunk) (Object) this).world;
        if (chunk.getBlockNibbles() == null) {
            chunk.setBlockNibbles(StarLightEngine.getFilledEmptyLight((net.minecraft.world.level.LevelHeightAccessor) world));
        }
        if (chunk.getSkyNibbles() == null) {
            chunk.setSkyNibbles(StarLightEngine.getFilledEmptyLight((net.minecraft.world.level.LevelHeightAccessor) world));
        }
    }
}
