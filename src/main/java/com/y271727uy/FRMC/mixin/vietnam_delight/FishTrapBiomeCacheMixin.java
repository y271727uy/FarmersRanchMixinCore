package com.y271727uy.FRMC.mixin.vietnam_delight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.huwng.vietnamdelight.block.entity.FishTrapBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(value = FishTrapBlockEntity.class, remap = false)
public abstract class FishTrapBiomeCacheMixin {
    @Unique private boolean frmc$riverResultCached;
    @Unique private boolean frmc$nearRiver;
    @Unique private long frmc$lastRiverCheck = -1;

    /** @author FRMC @reason Cache stable river biomes without loading neighboring chunks. */
    @Overwrite
    private boolean isNearRiverBiome(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (frmc$riverResultCached && (frmc$nearRiver ||
                serverLevel.getGameTime() - frmc$lastRiverCheck < 200)) {
            return frmc$nearRiver;
        }
        frmc$riverResultCached = true;
        frmc$lastRiverCheck = serverLevel.getGameTime();
        frmc$nearRiver = false;

        ChunkSource chunks = serverLevel.getChunkSource();
        int quartY = pos.getY() >> 2;
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                int x = pos.getX() + dx;
                int z = pos.getZ() + dz;
                ChunkAccess chunk = chunks.getChunkNow(x >> 4, z >> 4);
                if (chunk == null) {
                    continue;
                }
                Holder<Biome> biome = chunk.getNoiseBiome(x >> 2, quartY, z >> 2);
                if (biome.is(Biomes.RIVER)) {
                    frmc$nearRiver = true;
                    return true;
                }
            }
        }
        return false;
    }
}
