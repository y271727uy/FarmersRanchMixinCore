package com.y271727uy.FRMC.integration.acceleratedrendering.citadel;

import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.joml.Vector3f;

/** Collects Citadel model geometry in the format consumed by AR mesh builders. */
public final class CitadelArRenderer {
    private CitadelArRenderer() {
    }

    public static void collectTabula(ObjectList<?> cubes, VertexConsumer buffer, int packedOverlay) {
        for (Object cube : cubes) {
            for (Object quad : CitadelGeometryAccess.tabulaQuads(cube)) {
                Vector3f normal = (Vector3f) CitadelGeometryAccess.tabulaNormal(quad);
                for (Object vertex : CitadelGeometryAccess.tabulaVertices(quad)) {
                    emit(buffer, (Vector3f) CitadelGeometryAccess.tabulaPosition(vertex),
                            CitadelGeometryAccess.tabulaU(vertex), CitadelGeometryAccess.tabulaV(vertex),
                            normal, packedOverlay);
                }
            }
        }
    }

    public static void collectBasic(ObjectList<BasicModelPart.ModelBox> cubes,
                                    VertexConsumer buffer, int packedOverlay) {
        for (BasicModelPart.ModelBox cube : cubes) {
            for (Object quad : CitadelGeometryAccess.basicQuads(cube)) {
                Vector3f normal = (Vector3f) CitadelGeometryAccess.basicNormal(quad);
                for (Object vertex : CitadelGeometryAccess.basicVertices(quad)) {
                    emit(buffer, (Vector3f) CitadelGeometryAccess.basicPosition(vertex),
                            CitadelGeometryAccess.basicU(vertex), CitadelGeometryAccess.basicV(vertex),
                            normal, packedOverlay);
                }
            }
        }
    }

    private static void emit(VertexConsumer buffer, Vector3f position, float textureU, float textureV,
                             Vector3f normal, int packedOverlay) {
        buffer.vertex(position.x / 16.0F, position.y / 16.0F, position.z / 16.0F,
                1.0F, 1.0F, 1.0F, 1.0F, textureU, textureV,
                packedOverlay, 0, normal.x, normal.y, normal.z);
    }
}
