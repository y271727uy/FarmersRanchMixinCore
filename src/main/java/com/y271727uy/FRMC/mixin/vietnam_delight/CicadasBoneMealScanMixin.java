package com.y271727uy.FRMC.mixin.vietnam_delight;

import java.util.ArrayList;
import java.util.List;
import net.huwng.vietnamdelight.block.CicadasBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(value = CicadasBlock.class, remap = false)
public abstract class CicadasBoneMealScanMixin {
    /** @author FRMC @reason Bone-meal nearby crops without loading the 9x9x9 scan cube. */
    @Overwrite
    private static void boneMealRandomCropNearby(ServerLevel level, BlockPos origin, RandomSource random) {
        ChunkSource chunks = level.getChunkSource();
        List<BlockPos> growable = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    int x = origin.getX() + dx;
                    int y = origin.getY() + dy;
                    int z = origin.getZ() + dz;
                    ChunkAccess chunk = chunks.getChunkNow(x >> 4, z >> 4);
                    if (chunk == null) {
                        continue;
                    }
                    cursor.set(x, y, z);
                    BlockState state = chunk.getBlockState(cursor);
                    if (state.getBlock() instanceof BonemealableBlock
                            && ((BonemealableBlock) state.getBlock()).isValidBonemealTarget(level, cursor, state, false)) {
                        growable.add(cursor.immutable());
                    }
                }
            }
        }
        if (growable.isEmpty()) {
            return;
        }
        BlockPos chosen = growable.get(random.nextInt(growable.size()));
        BlockState state = level.getBlockState(chosen);
        if (state.getBlock() instanceof BonemealableBlock crop
                && crop.isBonemealSuccess(level, random, chosen, state)) {
            crop.performBonemeal(level, random, chosen, state);
        }
        level.levelEvent(1505, chosen, 0);
    }
}
