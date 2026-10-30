package com.y271727uy.FRMC.mixin.starlight;

import ca.spottedleaf.starlight.common.world.ExtendedWorld;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Bridges GuideME's synthetic level to Starlight's immediate chunk API. */
@Pseudo
@Mixin(targets = "guideme.scene.level.GuidebookLevel", remap = false)
public abstract class GuidebookLevelMixin implements ExtendedWorld {
    @Override
    public LevelChunk getChunkAtImmediately(int chunkX, int chunkZ) {
        return ((net.minecraft.world.level.Level) (Object) this).getChunkSource().getChunkNow(chunkX, chunkZ);
    }

    @Override
    public ChunkAccess getAnyChunkImmediately(int chunkX, int chunkZ) {
        return ((net.minecraft.world.level.Level) (Object) this).getChunkSource().getChunkNow(chunkX, chunkZ);
    }
}
