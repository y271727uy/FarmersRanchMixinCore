package com.y271727uy.FRMC.integration.citadel.render.embeddium;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.citadel.model.CitadelGeometryAccess;
import com.y271727uy.FRMC.integration.embeddium.vertex.EntityVertex;
import com.y271727uy.FRMC.mixin.minecraft.vertex.BufferBuilderAccessorMixin;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.minecraft.util.FastColor.ARGB32;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

/** Writes Citadel quads through Embeddium's native entity vertex writer. */
public final class CitadelEmbeddiumRenderer {
    /** Quads written per {@code push}; keeps each stack frame well under LWJGL's default 64 KiB stack. */
    private static final int MAX_BATCH_QUADS = 128;
    private static final boolean AR_PRESENT = detectAr();
    private static final ThreadLocal<float[]> TRANSFORMED = ThreadLocal.withInitial(() -> new float[0]);

    private CitadelEmbeddiumRenderer() {
    }

    public static boolean isSupported(VertexConsumer buffer) {
        if (VertexBufferWriter.tryOf(buffer) == null) {
            return false;
        }
        if (buffer instanceof BufferBuilder builder) {
            return ((BufferBuilderAccessorMixin) builder).frmc$getFormat() == DefaultVertexFormat.NEW_ENTITY;
        }
        if (buffer instanceof SodiumBufferBuilder sodium) {
            BufferBuilder original = sodium.getOriginalBufferBuilder();
            return ((BufferBuilderAccessorMixin) original).frmc$getFormat() == DefaultVertexFormat.NEW_ENTITY;
        }
        return false;
    }

    public static boolean isArAccelerated(VertexConsumer buffer) {
        if (!AR_PRESENT) {
            return false;
        }
        try {
            return ArAccess.isAccelerated(buffer);
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean detectAr() {
        try {
            Class.forName(ArAccess.class.getName(), true, CitadelEmbeddiumRenderer.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    /**
     * Isolated so the Accelerated Rendering classes are only linked when that mod exists;
     * a failed link here is caught by {@link #detectAr()} and treated as "not accelerated".
     */
    private static final class ArAccess {
        static {
            // Force resolution of both AR types now, so a missing mod fails during detection, not per call.
            com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension.class
                    .getName();
            com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer.class
                    .getName();
        }

        static boolean isAccelerated(VertexConsumer buffer) {
            var accelerated = com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders
                    .VertexConsumerExtension.getAccelerated(buffer);
            return accelerated != null && accelerated.isAccelerated();
        }
    }

    /**
     * Flattens the quads of {@code cubes}. Cube corners are shared by up to three faces, so positions are
     * de-duplicated (by exact float bits) and each quad stores four indices into the unique-position table.
     */
    public static Geometry snapshot(Object[] cubes, boolean tabula) {
        Map<PositionKey, Integer> unique = new HashMap<>();
        List<float[]> uniquePositions = new ArrayList<>();
        List<int[]> quadIndices = new ArrayList<>();
        List<float[]> quadUvs = new ArrayList<>();
        List<float[]> quadNormals = new ArrayList<>();
        for (Object cube : cubes) {
            Object[] cubeQuads = tabula ? CitadelGeometryAccess.tabulaQuads(cube) : CitadelGeometryAccess.basicQuads(cube);
            for (Object quad : cubeQuads) {
                if (quad == null) {
                    continue;
                }
                Object[] vertices = tabula ? CitadelGeometryAccess.tabulaVertices(quad) : CitadelGeometryAccess.basicVertices(quad);
                if (vertices.length != 4) {
                    continue;
                }
                Vector3f normal = (Vector3f) (tabula ? CitadelGeometryAccess.tabulaNormal(quad) : CitadelGeometryAccess.basicNormal(quad));
                int[] indices = new int[4];
                float[] uv = new float[8];
                for (int i = 0; i < 4; i++) {
                    Object vertex = vertices[i];
                    Vector3f position = (Vector3f) (tabula ? CitadelGeometryAccess.tabulaPosition(vertex) : CitadelGeometryAccess.basicPosition(vertex));
                    // Citadel stores cube vertices in model pixels, while the pose uses block units.
                    float x = position.x / 16.0F;
                    float y = position.y / 16.0F;
                    float z = position.z / 16.0F;
                    PositionKey key = new PositionKey(Float.floatToIntBits(x), Float.floatToIntBits(y), Float.floatToIntBits(z));
                    Integer index = unique.get(key);
                    if (index == null) {
                        index = uniquePositions.size();
                        unique.put(key, index);
                        uniquePositions.add(new float[] {x, y, z});
                    }
                    indices[i] = index;
                    uv[i * 2] = tabula ? CitadelGeometryAccess.tabulaU(vertex) : CitadelGeometryAccess.basicU(vertex);
                    uv[i * 2 + 1] = tabula ? CitadelGeometryAccess.tabulaV(vertex) : CitadelGeometryAccess.basicV(vertex);
                }
                quadIndices.add(indices);
                quadUvs.add(uv);
                quadNormals.add(new float[] {normal.x, normal.y, normal.z});
            }
        }

        int quadCount = quadIndices.size();
        float[] positions = new float[uniquePositions.size() * 3];
        for (int i = 0; i < uniquePositions.size(); i++) {
            System.arraycopy(uniquePositions.get(i), 0, positions, i * 3, 3);
        }
        int[] indices = new int[quadCount * 4];
        float[] uvs = new float[quadCount * 8];
        float[] normals = new float[quadCount * 3];
        for (int q = 0; q < quadCount; q++) {
            System.arraycopy(quadIndices.get(q), 0, indices, q * 4, 4);
            System.arraycopy(quadUvs.get(q), 0, uvs, q * 8, 8);
            System.arraycopy(quadNormals.get(q), 0, normals, q * 3, 3);
        }
        return new Geometry(cubes, positions, indices, uvs, normals, quadCount);
    }

    private record PositionKey(int x, int y, int z) {
    }

    public static final class Geometry {
        private final Object[] cubeIdentities;
        private final float[] positions;
        private final int[] indices;
        private final float[] uvs;
        private final float[] normals;
        private final int quadCount;

        private Geometry(Object[] cubes, float[] positions, int[] indices, float[] uvs, float[] normals,
                         int quadCount) {
            this.cubeIdentities = cubes.clone();
            this.positions = positions;
            this.indices = indices;
            this.uvs = uvs;
            this.normals = normals;
            this.quadCount = quadCount;
        }

        public boolean matchesCubes(java.util.List<?> cubes) {
            if (cubeIdentities.length != cubes.size()) {
                return false;
            }
            for (int i = 0; i < cubeIdentities.length; i++) {
                if (cubeIdentities[i] != cubes.get(i)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static void render(Geometry geometry, PoseStack.Pose pose, VertexConsumer buffer,
                              int packedLight, int packedOverlay, int color) {
        if (geometry.quadCount == 0) {
            return;
        }
        VertexBufferWriter writer = VertexBufferWriter.tryOf(buffer);
        if (writer == null) {
            return;
        }
        Matrix4f transform = pose.pose();
        Matrix3f normalTransform = pose.normal();
        int packedColor = ColorABGR.pack(ARGB32.red(color), ARGB32.green(color), ARGB32.blue(color), ARGB32.alpha(color));

        // Each unique corner is transformed once, instead of once per face that shares it.
        float[] source = geometry.positions;
        float[] transformed = TRANSFORMED.get();
        if (transformed.length < source.length) {
            transformed = new float[source.length];
            TRANSFORMED.set(transformed);
        }
        Vector3f position = new Vector3f();
        for (int i = 0; i < source.length; i += 3) {
            position.set(source[i], source[i + 1], source[i + 2]).mulPosition(transform);
            transformed[i] = position.x;
            transformed[i + 1] = position.y;
            transformed[i + 2] = position.z;
        }

        Vector3f normal = new Vector3f();
        int[] indices = geometry.indices;
        float[] uvs = geometry.uvs;
        float[] normals = geometry.normals;
        for (int first = 0; first < geometry.quadCount; first += MAX_BATCH_QUADS) {
            int batch = Math.min(MAX_BATCH_QUADS, geometry.quadCount - first);
            // One stack frame and one push per batch; Sodium's own entity renderer also pushes whole cuboids.
            try (MemoryStack stack = MemoryStack.stackPush()) {
                long base = stack.nmalloc(64, batch * 4 * EntityVertex.STRIDE);
                long ptr = base;
                for (int q = first; q < first + batch; q++) {
                    normal.set(normals[q * 3], normals[q * 3 + 1], normals[q * 3 + 2]).mul(normalTransform);
                    int packedNormal = NormI8.pack(normal);
                    for (int i = 0; i < 4; i++) {
                        int vertex = indices[q * 4 + i] * 3;
                        EntityVertex.write(ptr, transformed[vertex], transformed[vertex + 1], transformed[vertex + 2],
                                packedColor, uvs[q * 8 + i * 2], uvs[q * 8 + i * 2 + 1],
                                packedOverlay, packedLight, packedNormal);
                        ptr += EntityVertex.STRIDE;
                    }
                }
                writer.push(stack, base, batch * 4, EntityVertex.FORMAT);
            }
        }
    }
}
