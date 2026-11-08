package com.y271727uy.FRMC.integration.citadel.render.ar;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.mojang.blaze3d.systems.RenderSystem;
import com.y271727uy.FRMC.integration.oculus.OculusIntegration;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import org.joml.Matrix4f;

/** Selects one reduced mesh for small details on the main world entity model. */
public final class CitadelModelLod {
    private static final float DETAIL_RATIO = 0.25F;
    private static final float MAX_DETAIL_PIXELS = 2.0F;
    private static boolean worldEntity;
    private static EntityModel<?> baseModel;

    private final Matrix4f view = new Matrix4f();
    private final float[] inheritedScale = new float[Long.SIZE];
    private final float[] pivotReach = new float[Long.SIZE];

    public static boolean setWorldEntity(boolean active) {
        boolean previous = worldEntity;
        worldEntity = active;
        return previous;
    }

    public static EntityModel<?> setBaseModel(EntityModel<?> model) {
        EntityModel<?> previous = baseModel;
        baseModel = model;
        return previous;
    }

    static boolean activeFor(Object model) {
        return worldEntity && model == baseModel
                && !(OculusIntegration.IS_IRIS_INSTALLED && IrisApi.getInstance().isShaderPackInUse());
    }

    boolean canReduce(Plan plan, BasicModelPart[] nodes, int[] parents, Matrix4f base) {
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        RenderSystem.getModelViewMatrix().mul(base, view);
        var target = Minecraft.getInstance().getMainRenderTarget();
        return fits(plan, nodes, parents, view, projection, target.viewWidth, target.viewHeight);
    }

    boolean fits(Plan plan, BasicModelPart[] nodes, int[] parents, Matrix4f modelView,
                 Matrix4f projection, int width, int height) {
        float depth = -modelView.m32();
        if (!(depth >= 16.0F) || width <= 0 || height <= 0 || !modelView.isFinite()
                || (modelView.properties() & Matrix4f.PROPERTY_AFFINE) == 0 || !projection.isFinite()
                || projection.m23() != -1.0F || projection.m33() != 0.0F
                || projection.m03() != 0.0F || projection.m13() != 0.0F
                || projection.m01() != 0.0F || projection.m10() != 0.0F) {
            return false;
        }

        float reach = 0.0F;
        float diameter = 0.0F;
        int end = Long.SIZE - Long.numberOfLeadingZeros(plan.omitted());
        for (int i = 0; i < end; i++) {
            AdvancedModelBox node = (AdvancedModelBox) nodes[i];
            int parent = parents[i];
            float parentScale = parent < 0 ? 1.0F : inheritedScale[parent];
            float parentReach = parent < 0 ? 0.0F : pivotReach[parent];
            float localScale = Math.max(node.scaleX, Math.max(node.scaleY, node.scaleZ));
            float translation = (Math.abs(node.rotationPointX) + Math.abs(node.rotationPointY)
                    + Math.abs(node.rotationPointZ)) / 16.0F;
            float ownScale = parentScale * localScale;
            pivotReach[i] = parentReach + parentScale * translation;
            inheritedScale[i] = node.scaleChildren ? ownScale : parentScale;
            if ((plan.omitted() & (1L << i)) != 0) {
                reach = Math.max(reach, pivotReach[i] + ownScale * plan.radius()[i]);
                diameter = Math.max(diameter, ownScale * plan.diameter()[i]);
            }
        }

        // The Frobenius norm bounds stretch for affine scale, rotation and shear.
        float viewScale = (modelView.properties() & Matrix4f.PROPERTY_ORTHONORMAL) != 0 ? 1.0F
                : (float) Math.sqrt(modelView.m00() * modelView.m00() + modelView.m01() * modelView.m01()
                        + modelView.m02() * modelView.m02() + modelView.m10() * modelView.m10()
                        + modelView.m11() * modelView.m11() + modelView.m12() * modelView.m12()
                        + modelView.m20() * modelView.m20() + modelView.m21() * modelView.m21()
                        + modelView.m22() * modelView.m22());
        reach *= viewScale;
        float nearest = depth - reach;
        float nearPlane = projection.perspectiveNear();
        if (!(nearPlane > 0.0F && nearest > nearPlane)) {
            return false;
        }
        float focalPixels = Math.max(width * Math.abs(projection.m00()), height * Math.abs(projection.m11())) * 0.5F;
        float offAxis = Math.abs(modelView.m30()) + Math.abs(modelView.m31());
        float pixels = focalPixels * viewScale * diameter / nearest * (1.0F + (offAxis + reach) / nearest);
        return focalPixels > 0.0F && Float.isFinite(pixels) && pixels <= MAX_DETAIL_PIXELS;
    }

    record Plan(long omitted, int vertices, float[] diameter, float[] radius) {
    }

    static final class MeshBounds {
        private final float[] diameter;
        private final float[] radius;
        private final int[] counts;
        private float largest;

        MeshBounds(int bones) {
            diameter = new float[bones];
            radius = new float[bones];
            counts = new int[bones];
        }

        void add(int bone, int count, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            float x = maxX - minX, y = maxY - minY, z = maxZ - minZ;
            diameter[bone] = (float) Math.sqrt(x * x + y * y + z * z);
            radius[bone] = Math.max(Math.abs(minX), Math.abs(maxX)) + Math.max(Math.abs(minY), Math.abs(maxY))
                    + Math.max(Math.abs(minZ), Math.abs(maxZ));
            largest = Math.max(largest, diameter[bone]);
            counts[bone] = count;
        }

        Plan plan(int totalVertices) {
            if (!(largest > 0.0F) || !Float.isFinite(largest)) {
                return null;
            }
            long omitted = 0;
            int saved = 0;
            for (int i = 1; i < diameter.length; i++) {
                if (counts[i] != 0 && diameter[i] <= largest * DETAIL_RATIO) {
                    omitted |= 1L << i;
                    saved += counts[i];
                }
            }
            return saved >= Math.max(16, (totalVertices + 9) / 10) && saved < totalVertices
                    ? new Plan(omitted, totalVertices - saved, diameter, radius) : null;
        }
    }
}
