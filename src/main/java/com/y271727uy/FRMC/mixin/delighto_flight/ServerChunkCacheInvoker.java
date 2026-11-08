package com.y271727uy.FRMC.mixin.delighto_flight;

import com.mojang.datafixers.util.Either;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerChunkCache.class)
public interface ServerChunkCacheInvoker {
    @Invoker("getChunkFutureMainThread")
    CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> frmc$requestChunk(
            int chunkX, int chunkZ, ChunkStatus status, boolean load);
}
