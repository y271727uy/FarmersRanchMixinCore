package com.y271727uy.FRMC.integration.oculus;

import com.mojang.blaze3d.vertex.PoseStack;
import com.y271727uy.FRMC.integration.embeddium.EmbeddiumPortConfig;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;

/**
 * 光影下阴影贴图每 N 帧渲染一次。功能来自 Emberlens（Oculus 的 fork，LGPL-3.0），实现是重写的。
 * <p>
 * 跳过的帧里阴影贴图保持上一次的内容，着色器却是用“相对当前相机的位置”去查它的。
 * 所以跳过的帧不能直接沿用旧的阴影模型视图矩阵，而要在其右侧乘上相机这期间的位移：
 * {@code MV0 * (W - C0) == (MV0 * T(C1 - C0)) * (W - C1)}。fork 里没有这一步，移动时阴影会抖。
 */
public final class ShadowUpdateInterval {
    /** 相机瞬移超过这个距离（格）就不再沿用旧阴影。 */
    private static final double MAX_REUSE_DISTANCE_SQ = 8.0 * 8.0;

    private static final Matrix4f CAPTURED_POSE = new Matrix4f();
    private static final Matrix3f CAPTURED_NORMAL = new Matrix3f();
    private static final Vector3d CAPTURED_CAMERA = new Vector3d();

    private static Object owner;
    private static boolean hasCapture;
    private static int framesSinceRender;
    private static boolean skipping;

    private ShadowUpdateInterval() {
    }

    /** 每帧开头复位；本帧如果没走到 {@link #decide}，就按正常渲染处理。 */
    public static void beginFrame() {
        skipping = false;
    }

    /**
     * 决定本帧是否跳过阴影渲染。
     *
     * @param pipeline   当前渲染管线，换了管线（换维度、重载光影）必须重新渲染
     * @param mustRender 管线自身要求本帧必须渲染
     */
    public static boolean decide(Object pipeline, boolean mustRender, Vector3d camera) {
        int interval = EmbeddiumPortConfig.shadowUpdateInterval();
        boolean reuse = interval > 1
            && !mustRender
            && pipeline == owner
            && hasCapture
            && framesSinceRender < interval - 1
            && camera.distanceSquared(CAPTURED_CAMERA) <= MAX_REUSE_DISTANCE_SQ;

        if (reuse) {
            framesSinceRender++;
        } else {
            framesSinceRender = 0;
            owner = pipeline;
        }
        skipping = reuse;
        return reuse;
    }

    public static boolean skipping() {
        return skipping;
    }

    /** 记录本次真正用于渲染阴影的矩阵和相机位置。 */
    public static void capture(PoseStack.Pose pose, Vector3d camera) {
        CAPTURED_POSE.set(pose.pose());
        CAPTURED_NORMAL.set(pose.normal());
        CAPTURED_CAMERA.set(camera);
        hasCapture = true;
    }

    /** 跳过的帧里提供给着色器的阴影模型视图矩阵：旧矩阵加上相机位移补偿。 */
    public static PoseStack compensated(Vector3d camera) {
        PoseStack stack = new PoseStack();
        stack.last().pose().set(CAPTURED_POSE).translate(
            (float) (camera.x - CAPTURED_CAMERA.x),
            (float) (camera.y - CAPTURED_CAMERA.y),
            (float) (camera.z - CAPTURED_CAMERA.z));
        stack.last().normal().set(CAPTURED_NORMAL);
        return stack;
    }
}
