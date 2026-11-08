package com.y271727uy.FRMC.integration.delighto_flight;

import com.cloudmeow.delightoflight.registry.DFBlocks;
import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.mixin.delighto_flight.ServerChunkCacheInvoker;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.slf4j.Logger;
import vectorwing.farmersdelight.common.registry.ModBlocks;

public final class LightningSoilConversion {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int RADIUS = 3;
    private static final int CHUNK_TIMEOUT_SECONDS = 30;
    private static final TicketType<Long> STRIKE_TICKET =
            TicketType.create("frmc_weather_soil", Long::compare, CHUNK_TIMEOUT_SECONDS * 20);
    private static final AtomicLong NEXT_TICKET = new AtomicLong();

    private LightningSoilConversion() {
    }

    public static void apply(ServerLevel level, BlockPos center) {
        Strike strike = Strike.at(center);
        if (strike.convertIfLoaded(level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        // A join event can itself run during chunk loading. Enqueue before requesting any tickets.
        server.tell(new TickTask(server.getTickCount(), () -> {
            if (server.getLevel(level.dimension()) == level && !strike.convertIfLoaded(level)) {
                strike.requestChunks(level, server);
            }
        }));
    }

    private record Strike(BlockPos center, int minChunkX, int minChunkZ, int width, int depth) {
        static Strike at(BlockPos center) {
            int minX = SectionPos.blockToSectionCoord(center.getX() - RADIUS);
            int minZ = SectionPos.blockToSectionCoord(center.getZ() - RADIUS);
            int maxX = SectionPos.blockToSectionCoord(center.getX() + RADIUS);
            int maxZ = SectionPos.blockToSectionCoord(center.getZ() + RADIUS);
            return new Strike(center.immutable(), minX, minZ, maxX - minX + 1, maxZ - minZ + 1);
        }

        boolean convertIfLoaded(ServerLevel level) {
            ServerChunkCache source = level.getChunkSource();
            ChunkAccess[] chunks = new ChunkAccess[width * depth];
            for (int i = 0; i < chunks.length; i++) {
                chunks[i] = source.getChunkNow(minChunkX + i / depth, minChunkZ + i % depth);
                if (chunks[i] == null) {
                    return false;
                }
            }
            convert(level, chunks);
            return true;
        }

        void requestChunks(ServerLevel level, MinecraftServer server) {
            ServerChunkCache source = level.getChunkSource();
            Long ticket = NEXT_TICKET.incrementAndGet();
            List<ChunkPos> held = new ArrayList<>(width * depth);
            List<CompletableFuture<?>> requests = new ArrayList<>(width * depth);
            try {
                for (int i = 0; i < width * depth; i++) {
                    ChunkPos pos = new ChunkPos(minChunkX + i / depth, minChunkZ + i % depth);
                    source.addRegionTicket(STRIKE_TICKET, pos, 0, ticket);
                    held.add(pos);
                    requests.add(((ServerChunkCacheInvoker) source).frmc$requestChunk(pos.x, pos.z,
                            ChunkStatus.FULL, true));
                }
            } catch (RuntimeException failure) {
                releaseTickets(source, held, ticket);
                throw failure;
            }

            CompletableFuture<Void> ready = CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new))
                    .orTimeout(CHUNK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            ready.whenCompleteAsync((ignored, failure) -> {
                try {
                    if (failure != null) {
                        LOGGER.warn("Failed to prepare chunks for weather soil at {}", center, failure);
                    } else if (server.getLevel(level.dimension()) == level) {
                        // The ticket can expire before this callback gets to run.
                        convertIfLoaded(level);
                    }
                } finally {
                    releaseTickets(source, held, ticket);
                }
            }, server);
        }

        private void convert(ServerLevel level, ChunkAccess[] chunks) {
            Block farmland = ModBlocks.RICH_SOIL_FARMLAND.get();
            Block soil = ModBlocks.RICH_SOIL.get();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = center.getX() - RADIUS; x <= center.getX() + RADIUS; x++) {
                int column = (SectionPos.blockToSectionCoord(x) - minChunkX) * depth;
                for (int y = center.getY() - 1; y <= center.getY() + 1; y++) {
                    if (level.isOutsideBuildHeight(y)) {
                        continue;
                    }
                    for (int z = center.getZ() - RADIUS; z <= center.getZ() + RADIUS; z++) {
                        ChunkAccess chunk = chunks[column + SectionPos.blockToSectionCoord(z) - minChunkZ];
                        BlockState state = chunk.getBlockState(pos.set(x, y, z));
                        if (state.is(farmland)) {
                            level.setBlockAndUpdate(pos.immutable(), DFBlocks.WEATHER_SOIL_FARMLAND.get().defaultBlockState());
                        } else if (state.is(soil)) {
                            level.setBlockAndUpdate(pos.immutable(), DFBlocks.WEATHER_SOIL.get().defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    private static void releaseTickets(ServerChunkCache source, List<ChunkPos> chunks, Long ticket) {
        for (ChunkPos pos : chunks) {
            source.removeRegionTicket(STRIKE_TICKET, pos, 0, ticket);
        }
    }
}
