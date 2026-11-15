package com.y271727uy.FRMC.integration.embeddium.perf;

import com.y271727uy.FRMC.integration.embeddium.EmbeddiumPortConfig;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;

/**
 * 每帧区块网格上传量的预算。
 *
 * <p>原版 Embeddium 每帧把构建线程已完成的结果全部上传，多个线程同时交付时会在一帧里集中拷贝大量顶点数据。
 * 这里只限制“这一帧取走多少”，取不完的留在原队列里下一帧继续，不改动上传本身。
 */
public final class ChunkUploadBudget {
    /** 排队结果超过这个数量时不再限流，避免高速移动时积压越来越多、区块迟迟不出现。 */
    public static final int MAX_BACKLOG = 64;

    private ChunkUploadBudget() {
    }

    /** 每帧预算（字节），0 表示不限制，即原版行为。 */
    public static long bytesPerFrame() {
        return EmbeddiumPortConfig.chunkUploadBudgetKb() * 1024L;
    }

    public static long sizeOf(ChunkBuildOutput output) {
        long bytes = 0L;
        for (BuiltSectionMeshParts mesh : output.meshes.values()) {
            bytes += length(mesh.getVertexData()) + length(mesh.getIndexData());
        }
        return bytes;
    }

    private static int length(NativeBuffer buffer) {
        return buffer == null ? 0 : buffer.getLength();
    }
}
