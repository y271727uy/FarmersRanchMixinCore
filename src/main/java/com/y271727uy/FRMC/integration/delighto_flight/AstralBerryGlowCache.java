package com.y271727uy.FRMC.integration.delighto_flight;

import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/** Remembers the last glow scan for each head in a loaded chunk. */
public final class AstralBerryGlowCache {
    private static final Map<LevelChunk, Int2ObjectOpenHashMap<Int2ByteOpenHashMap>> CHUNKS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private AstralBerryGlowCache() {
    }

    public static boolean shouldScan(ServerLevel level, BlockPos head) {
        LevelChunk chunk = loadedChunk(level, head);
        if (chunk == null) {
            return true;
        }
        var columns = CHUNKS.computeIfAbsent(chunk, ignored -> new Int2ObjectOpenHashMap<>());
        var heads = columns.computeIfAbsent(columnIndex(head), ignored -> new Int2ByteOpenHashMap());
        byte phase = (byte) (level.isNight() ? 2 : 1);
        // Record before scanning. Any write made during the scan invalidates the entry again.
        return heads.put(head.getY(), phase) != phase;
    }

    public static void invalidateColumn(ServerLevel level, BlockPos changed) {
        LevelChunk chunk = loadedChunk(level, changed);
        if (chunk != null) {
            var columns = CHUNKS.get(chunk);
            if (columns != null) {
                columns.remove(columnIndex(changed));
            }
        }
    }

    private static LevelChunk loadedChunk(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()));
    }

    private static int columnIndex(BlockPos pos) {
        return SectionPos.sectionRelative(pos.getX()) * 16 + SectionPos.sectionRelative(pos.getZ());
    }
}
