package com.y271727uy.FRMC.integration.sereneseasons;

import com.y271727uy.FRMC.mixin.sereneseasons.ChunkMapInvokerMixin;
import com.y271727uy.FRMC.mixin.sereneseasons.IceBlockInvokerMixin;
import com.y271727uy.FRMC.mixin.sereneseasons.ServerLevelInvokerMixin;
import com.y271727uy.FRMC.mixin.sereneseasons.BiomeInvokerMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.Mth;

import sereneseasons.init.ModTags;

/** Executes Serene Seasons' melt attempts on chunks already selected by vanilla. */
public final class SereneSeasonsMeltOptimizer {
    private SereneSeasonsMeltOptimizer() {
    }

    public static void tickChunk(ServerLevel level, LevelChunk chunk) {
        SeasonSnapshotCache.Snapshot snapshot = SeasonSnapshotCache.get(level);
        if (!snapshot.generateSnowAndIce()
                || !snapshot.dimensionWhitelisted()
                || snapshot.properties().meltRolls() <= 0
                || snapshot.properties().meltChance() <= 0.0F
                || !isSereneSeasonsActiveChunk(level, chunk)) {
            return;
        }
        float meltChance = snapshot.properties().meltChance() / 100.0F;
        for (int roll = 0; roll < snapshot.properties().meltRolls(); roll++) {
            meltInChunk(level, chunk, meltChance, snapshot);
        }
    }

    private static boolean isSereneSeasonsActiveChunk(ServerLevel level, LevelChunk chunk) {
        var chunkPos = chunk.getPos();
        var chunkMap = level.getChunkSource().chunkMap;
        return ((ChunkMapInvokerMixin) (Object) chunkMap).frmc$anyPlayerCloseEnoughForSpawning(chunkPos)
            && ((ServerLevelInvokerMixin) (Object) level)
                .frmc$isPositionTickingWithEntitiesLoaded(chunkPos.toLong());
    }

    private static void meltInChunk(
            ServerLevel level,
            LevelChunk chunk,
            float meltChance,
            SeasonSnapshotCache.Snapshot snapshot) {
        if (level.random.nextFloat() >= meltChance) {
            return;
        }

        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();
        BlockPos randomPos = level.getBlockRandomPos(minBlockX, 0, minBlockZ, 15);
        BlockPos topPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, randomPos);
        BlockPos belowPos = topPos.below();
        BlockState topState = level.getBlockState(topPos);
        BlockState belowState = level.getBlockState(belowPos);
        Holder<Biome> topBiome = level.getBiome(topPos);
        Holder<Biome> belowBiome = level.getBiome(belowPos);
        if (!topBiome.is(ModTags.Biomes.BLACKLISTED_BIOMES)
                && getBiomeTemperature(snapshot, topBiome, belowPos) >= 0.15F
                && topState.getBlock() == Blocks.SNOW) {
            level.setBlockAndUpdate(topPos, Blocks.AIR.defaultBlockState());
        }
        if (!belowBiome.is(ModTags.Biomes.BLACKLISTED_BIOMES)
                && getBiomeTemperature(snapshot, belowBiome, belowPos) >= 0.15F
                && belowState.getBlock() == Blocks.ICE) {
            ((IceBlockInvokerMixin) (Object) Blocks.ICE).frmc$invokeMelt(belowState, level, belowPos);
        }
    }

    private static float getBiomeTemperature(
            SeasonSnapshotCache.Snapshot snapshot,
            Holder<Biome> biome,
            BlockPos pos) {
        Biome value = biome.value();
        float temperature = ((BiomeInvokerMixin) (Object) value).frmc$getTemperature(pos);
        if (!biome.is(ModTags.Biomes.TROPICAL_BIOMES)
                && value.getBaseTemperature() <= 0.8F
                && !biome.is(ModTags.Biomes.BLACKLISTED_BIOMES)) {
            temperature = Mth.clamp(
                temperature + snapshot.properties().biomeTempAdjustment(),
                -0.5F,
                2.0F
            );
        }
        return temperature;
    }

}
