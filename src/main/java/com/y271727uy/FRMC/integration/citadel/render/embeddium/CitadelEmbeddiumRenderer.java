package com.y271727uy.FRMC.integration.citadel.render.embeddium;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.citadel.model.CitadelGeometryAccess;
import com.y271727uy.FRMC.integration.embeddium.vertex.EntityVertex;
import com.y271727uy.FRMC.mixin.minecraft.vertex.BufferBuilderAccessorMixin;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder;
import java.lang.reflect.Method;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.minecraft.util.FastColor.ARGB32;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

/** Writes Citadel quads through Embeddium's native entity vertex writer. */
public final class CitadelEmbeddiumRenderer {
    private static final long SCRATCH_BUFFER = MemoryUtil.nmemAlignedAlloc(64, 4L * EntityVertex.STRIDE);
    private static final Method AR_GET_ACCELERATED = findArMethod(
            "com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension",
            "getAccelerated", VertexConsumer.class);
    private static final Method AR_IS_ACCELERATED = findArMethod(
            "com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer",
            "isAccelerated");

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
        if (AR_GET_ACCELERATED == null || AR_IS_ACCELERATED == null) {
            return false;
        }
        try {
            Object accelerated = AR_GET_ACCELERATED.invoke(null, buffer);
            return accelerated != null && (boolean) AR_IS_ACCELERATED.invoke(accelerated);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static Method findArMethod(String className, String methodName, Class<?>... parameterTypes) {
        try {
            Class<?> type = Class.forName(className, false, CitadelEmbeddiumRenderer.class.getClassLoader());
            return type.getMethod(methodName, parameterTypes);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    public static Geometry snapshot(Object[] cubes, boolean tabula) {
        java.util.ArrayList<QuadData> quads = new java.util.ArrayList<>();
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
                float[] positions = new float[12];
                float[] uv = new float[8];
                for (int i = 0; i < 4; i++) {
                    Object vertex = vertices[i];
                    Vector3f position = (Vector3f) (tabula ? CitadelGeometryAccess.tabulaPosition(vertex) : CitadelGeometryAccess.basicPosition(vertex));
                    positions[i * 3] = position.x;
                    positions[i * 3 + 1] = position.y;
                    positions[i * 3 + 2] = position.z;
                    uv[i * 2] = tabula ? CitadelGeometryAccess.tabulaU(vertex) : CitadelGeometryAccess.basicU(vertex);
                    uv[i * 2 + 1] = tabula ? CitadelGeometryAccess.tabulaV(vertex) : CitadelGeometryAccess.basicV(vertex);
                }
                quads.add(new QuadData(positions, uv, normal.x, normal.y, normal.z));
            }
        }
        return new Geometry(cubes, quads.toArray(QuadData[]::new));
    }

    public static final class Geometry {
        private final Object[] cubeIdentities;
        private final QuadData[] quads;

        private Geometry(Object[] cubes, QuadData[] quads) {
            this.cubeIdentities = cubes.clone();
            this.quads = quads;
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

    private record QuadData(float[] positions, float[] uv, float normalX, float normalY, float normalZ) {
    }

    public static void render(Geometry geometry, PoseStack.Pose pose, VertexConsumer buffer,
                              int packedLight, int packedOverlay, int color) {
        VertexBufferWriter writer = VertexBufferWriter.tryOf(buffer);
        if (writer == null) {
            return;
        }
        Matrix4f transform = pose.pose();
        Matrix3f normalTransform = pose.normal();
        Vector3f normal = new Vector3f();
        Vector3f position = new Vector3f();
        int packedColor = ColorABGR.pack(ARGB32.red(color), ARGB32.green(color), ARGB32.blue(color), ARGB32.alpha(color));
        for (QuadData quad : geometry.quads) {
            normal.set(quad.normalX, quad.normalY, quad.normalZ).mul(normalTransform);
            int packedNormal = NormI8.pack(normal);
            long ptr = SCRATCH_BUFFER;
            for (int i = 0; i < 4; i++) {
                position.set(quad.positions[i * 3], quad.positions[i * 3 + 1], quad.positions[i * 3 + 2]).mulPosition(transform);
                EntityVertex.write(ptr, position.x, position.y, position.z, packedColor,
                        quad.uv[i * 2], quad.uv[i * 2 + 1], packedOverlay, packedLight, packedNormal);
                ptr += EntityVertex.STRIDE;
            }
            writer.push(null, SCRATCH_BUFFER, 4, EntityVertex.FORMAT);
        }
    }
}
