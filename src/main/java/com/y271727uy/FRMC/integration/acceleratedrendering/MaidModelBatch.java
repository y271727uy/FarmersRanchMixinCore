package com.y271727uy.FRMC.integration.acceleratedrendering;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.IAccelerationHolder;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.core.meshes.MeshBuffer;
import com.github.argon4w.acceleratedrendering.core.meshes.MeshType;
import com.github.argon4w.acceleratedrendering.core.meshes.ServerMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.SimpleMeshCollector;
import com.github.argon4w.acceleratedrendering.core.utils.ByteBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.utils.RenderTypeUtils;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.mods.ModsFeature;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.GeckoEntityChairRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.GeckoEntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoModel;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.acceleratedrendering.ArModelMeshUpload.Format;
import com.y271727uy.FRMC.integration.citadel.render.ar.ICitadelMeshSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class MaidModelBatch {
    private static final int MAX_BONES = 128;
    private static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final int VERTEX_BYTES = ArModelMeshUpload.VERTEX_BYTES;
    private static final MaidModelBatch SESSION = new MaidModelBatch();
    private static final Map<GeoModel, List<Variant>> MESHES = new IdentityHashMap<>();
    private static MeshBuffer storage;

    private final AnimatedGeoBone[] bones = new AnimatedGeoBone[MAX_BONES];
    private final Matrix4f[] poses = new Matrix4f[MAX_BONES];
    private final Matrix3f[] normals = new Matrix3f[MAX_BONES];
    private PoseStack stack;
    private GeoModel model;
    private AcceleratedBufferBuilder builder;
    private Format format;
    private int size;
    private int light;
    private int overlay;
    private int color;
    private boolean recording;

    private MaidModelBatch() {
        Arrays.setAll(poses, index -> new Matrix4f());
        Arrays.setAll(normals, index -> new Matrix3f());
    }

    public static MaidModelBatch begin(Object renderer, AnimatedGeoModel model, PoseStack stack, int light) {
        if (!RenderSystem.isOnRenderThread() || SESSION.stack != null
                || (renderer.getClass() != GeckoEntityMaidRenderer.class
                        && renderer.getClass() != GeckoEntityChairRenderer.class)
                || !CoreFeature.isLoaded() || !CoreFeature.isRenderingLevel()
                || !AcceleratedEntityRenderingFeature.isEnabled()
                || !AcceleratedEntityRenderingFeature.shouldUseAcceleratedPipeline()
                || AcceleratedEntityRenderingFeature.getMeshType() != MeshType.SERVER
                || !ModsFeature.isEnabled() || !ModsFeature.shouldAccelerateTlm()) {
            return null;
        }
        SESSION.stack = stack;
        SESSION.model = model.geoModel();
        SESSION.light = light;
        SESSION.recording = true;
        return SESSION;
    }

    public static boolean capture(AnimatedGeoBone bone, PoseStack stack, VertexConsumer buffer,
                                  int light, int overlay, int color) {
        MaidModelBatch batch = SESSION;
        if (!batch.recording || batch.stack != stack) {
            return false;
        }
        var accelerated = buffer instanceof IAccelerationHolder holder ? holder.getAccelerated() : buffer;
        PoseStack.Pose pose = stack.last();
        if (!(accelerated instanceof AcceleratedBufferBuilder builder) || light != batch.light
                || !pose.pose().isFinite() || !pose.normal().isFinite() || batch.size == MAX_BONES
                || batch.builder != null && (batch.builder != builder || batch.overlay != overlay || batch.color != color)) {
            batch.replay();
            return false;
        }
        if (batch.builder == null) {
            RenderType material = ArModelMeshUpload.unwrap(builder.getRenderType());
            var texture = RenderTypeUtils.getTextureLocation(material);
            boolean translucent = CoreFeature.shouldForceAccelerateTranslucent() && texture != null
                    && (material == RenderType.entityTranslucent(texture, true)
                            || material == RenderType.entityTranslucent(texture, false));
            if ((!translucent && !ArModelMeshUpload.isCutout(material))
                    || !(builder instanceof ICitadelMeshSource source) || !source.frmc$canSubmitGpuPoses()) {
                batch.recording = false;
                return false;
            }
            batch.format = ArModelMeshUpload.formatFor(builder, translucent);
            if (batch.format == null) {
                batch.recording = false;
                return false;
            }
            batch.builder = builder;
            batch.overlay = overlay;
            batch.color = color;
        }
        if (bone.geoBone().cubes().getCubeCount() != 0) {
            int index = batch.size++;
            batch.bones[index] = bone;
            batch.poses[index].set(pose.pose());
            batch.normals[index].set(pose.normal());
        }
        return true;
    }

    public void submit() {
        recording = false;
        if (size == 0) {
            return;
        }
        ICitadelMeshSource source = (ICitadelMeshSource) builder;
        ServerMesh mesh = size > 1 && source.frmc$canSubmitGpuPoses() ? mesh() : null;
        if (mesh == null) {
            replay();
            return;
        }
        try {
            builder.beginTransform(poses[0], normals[0]);
            int first = source.frmc$activeGpuPose();
            for (int i = 1; i < size; i++) {
                builder.beginTransform(poses[i], normals[i]);
            }
            source.frmc$selectGpuPose(first);
            mesh.write(builder, color, light, overlay);
        } finally {
            builder.endTransform();
        }
    }

    @SuppressWarnings("unchecked")
    private void replay() {
        recording = false;
        for (int i = 0; i < size; i++) {
            var renderer = (IAcceleratedRenderer<AnimatedGeoBone>) bones[i].geoBone();
            builder.doRender(renderer, bones[i], poses[i], normals[i], light, overlay, color);
        }
        Arrays.fill(bones, 0, size, null);
        size = 0;
    }

    private ServerMesh mesh() {
        List<Variant> variants = MESHES.computeIfAbsent(model, ignored -> new ArrayList<>());
        for (Variant variant : variants) {
            if (variant.matches(bones, size, format)) {
                return variant.mesh;
            }
        }
        if (variants.size() >= 16 || !format.initialize()) {
            return null;
        }
        SimpleMeshCollector collector = new SimpleMeshCollector(builder.getLayout());
        try (ByteBufferBuilder data = collector.getBuffer(); ByteBufferBuilder merged = new ByteBufferBuilder(4096)) {
            VertexConsumer writer = builder.decorate(collector);
            int[] offsets = new int[size + 1];
            for (int i = 0; i < size; i++) {
                MaidBoneGeometry.emit(bones[i], writer);
                long count = collector.getVertexCount();
                if (count * VERTEX_BYTES > MAX_BYTES) {
                    return null;
                }
                offsets[i + 1] = (int) count;
            }
            collector.flush();
            int count = offsets[size];
            long bytes = (long) count * VERTEX_BYTES;
            if (count == 0) {
                return null;
            }
            if (storage == null) {
                storage = new MeshBuffer(64L);
            }
            if (storage.getPosition() + bytes > MAX_BYTES || storage.overflow(bytes)) {
                return null;
            }
            try (var source = data.build()) {
                long address = MemoryUtil.memAddress(source.byteBuffer());
                for (int i = 0; i < size; i++) {
                    ArModelMeshUpload.appendVertices(merged, address + (long) offsets[i] * format.vertexBytes,
                            offsets[i + 1] - offsets[i], format, i);
                }
            }
            try (var result = merged.build()) {
                int offset = (int) (storage.append(MemoryUtil.memAddress(result.byteBuffer()), bytes) / VERTEX_BYTES);
                ServerMesh mesh = new ServerMesh(0, ServerMesh.Builder.COUNTER++, count, offset, true, storage);
                ArModelMeshUpload.register(mesh, format, null);
                GeoBone[] geometry = new GeoBone[size];
                Arrays.setAll(geometry, index -> bones[index].geoBone());
                variants.add(new Variant(geometry, format, mesh));
                return mesh;
            }
        }
    }

    public void clear() {
        Arrays.fill(bones, 0, size, null);
        size = 0;
        recording = false;
        stack = null;
        builder = null;
        model = null;
    }

    public static void release() {
        MESHES.clear();
        if (storage != null) {
            storage.delete();
            storage = null;
        }
    }

    private record Variant(GeoBone[] bones, Format format, ServerMesh mesh) {
        boolean matches(AnimatedGeoBone[] visible, int size, Format format) {
            if (this.format != format || bones.length != size) {
                return false;
            }
            for (int i = 0; i < size; i++) {
                if (bones[i] != visible[i].geoBone()) {
                    return false;
                }
            }
            return true;
        }
    }
}
