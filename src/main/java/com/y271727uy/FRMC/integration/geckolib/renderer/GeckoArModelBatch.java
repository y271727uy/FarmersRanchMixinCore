package com.y271727uy.FRMC.integration.geckolib.renderer;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.IAccelerationHolder;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.meshes.MeshBuffer;
import com.github.argon4w.acceleratedrendering.core.meshes.MeshType;
import com.github.argon4w.acceleratedrendering.core.meshes.ServerMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.SimpleMeshCollector;
import com.github.argon4w.acceleratedrendering.core.utils.ByteBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.utils.RenderTypeUtils;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.mods.ModsFeature;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.acceleratedrendering.ArModelMeshUpload;
import com.y271727uy.FRMC.integration.acceleratedrendering.ArModelMeshUpload.Format;
import com.y271727uy.FRMC.integration.citadel.render.ar.ICitadelMeshSource;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import static com.y271727uy.FRMC.integration.acceleratedrendering.ArModelMeshUpload.VERTEX_BYTES;

/** Records the poses produced by GeckoLib and submits one cached mesh for the visible bones. */
public final class GeckoArModelBatch {
    private static final int MAX_BONES = 128;
    private static final int MAX_VARIANTS = 16;
    private static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final GeckoArModelBatch SESSION = new GeckoArModelBatch();
    private static final Map<BakedGeoModel, List<Variant>> MESHES = new IdentityHashMap<>();
    private static MeshBuffer storage;

    private static final ClassValue<Boolean> STOCK_TRAVERSAL = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                Class<?> recursive = recursiveMethodOwner(type, GeoAnimatable.class);
                return (recursive == GeoRenderer.class || recursive == GeoEntityRenderer.class
                        && recursiveMethodOwner(type, Entity.class) == GeoEntityRenderer.class)
                        && type.getMethod("renderChildBones", PoseStack.class, GeoAnimatable.class, GeoBone.class,
                                RenderType.class, MultiBufferSource.class, VertexConsumer.class,
                                boolean.class, float.class, int.class, int.class,
                                float.class, float.class, float.class, float.class).getDeclaringClass() == GeoRenderer.class
                        && type.getMethod("renderCubesOfBone", PoseStack.class, GeoBone.class, VertexConsumer.class,
                                int.class, int.class, float.class, float.class, float.class, float.class)
                                .getDeclaringClass() == GeoRenderer.class
                        && type.getMethod("renderCube", PoseStack.class, GeoCube.class, VertexConsumer.class,
                                int.class, int.class, float.class, float.class, float.class, float.class)
                                .getDeclaringClass() == GeoRenderer.class
                        && type.getMethod("applyRenderLayersForBone", PoseStack.class, GeoAnimatable.class,
                                GeoBone.class, RenderType.class, MultiBufferSource.class, VertexConsumer.class,
                                float.class, int.class, int.class).getDeclaringClass() == GeoRenderer.class;
            } catch (NoSuchMethodException exception) {
                return false;
            }
        }
    };

    private static final ClassValue<Boolean> NO_BONE_LAYER = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("renderForBone", PoseStack.class, GeoAnimatable.class, GeoBone.class,
                        RenderType.class, MultiBufferSource.class, VertexConsumer.class, float.class, int.class,
                        int.class).getDeclaringClass() == GeoRenderLayer.class;
            } catch (NoSuchMethodException exception) {
                return false;
            }
        }
    };

    private final GeoBone[] bones = new GeoBone[MAX_BONES];
    private final Matrix4f[] positions = new Matrix4f[MAX_BONES];
    private final Matrix3f[] normals = new Matrix3f[MAX_BONES];
    private final PoseStack fallbackPose = new PoseStack();
    private GeoRenderer<?> renderer;
    private BakedGeoModel model;
    private VertexConsumer consumer;
    private PoseStack poseStack;
    private AcceleratedBufferBuilder builder;
    private Format format;
    private boolean recording;
    private int size;
    private int light;
    private int overlay;
    private float red, green, blue, alpha;

    private GeckoArModelBatch() {
        for (int i = 0; i < MAX_BONES; i++) {
            positions[i] = new Matrix4f();
            normals[i] = new Matrix3f();
        }
    }

    private static Class<?> recursiveMethodOwner(Class<?> type, Class<?> animatable) throws NoSuchMethodException {
        return type.getMethod("renderRecursively", PoseStack.class, animatable, GeoBone.class,
                RenderType.class, MultiBufferSource.class, VertexConsumer.class, boolean.class,
                float.class, int.class, int.class, float.class, float.class, float.class, float.class)
                .getDeclaringClass();
    }

    public static GeckoArModelBatch begin(GeoRenderer<?> renderer, BakedGeoModel model, PoseStack poseStack,
                                          VertexConsumer consumer, boolean isReRender, int light, int overlay,
                                          float red, float green, float blue, float alpha) {
        if (!RenderSystem.isOnRenderThread() || SESSION.renderer != null
                || !CoreFeature.isLoaded() || !CoreFeature.isRenderingLevel()
                || !AcceleratedEntityRenderingFeature.isEnabled()
                || !AcceleratedEntityRenderingFeature.shouldUseAcceleratedPipeline()
                || AcceleratedEntityRenderingFeature.getMeshType() != MeshType.SERVER
                || !ModsFeature.isEnabled() || !ModsFeature.shouldAccelerateGecko()) {
            return null;
        }
        var accelerated = consumer instanceof IAccelerationHolder holder ? holder.getAccelerated() : consumer;
        if (!(accelerated instanceof AcceleratedBufferBuilder builder)
                || !(builder instanceof ICitadelMeshSource source) || !source.frmc$canSubmitGpuPoses()) {
            return null;
        }
        RenderType material = ArModelMeshUpload.unwrap(builder.getRenderType());
        boolean translucent = CoreFeature.shouldForceAccelerateTranslucent() && isEntityTranslucent(material);
        if (!translucent && !ArModelMeshUpload.isCutout(material)) {
            return null;
        }
        Format format = ArModelMeshUpload.formatFor(builder, translucent);
        if (format == null || !STOCK_TRAVERSAL.get(renderer.getClass())) {
            return null;
        }
        if (!isReRender) {
            for (GeoRenderLayer<?> layer : renderer.getRenderLayers()) {
                if (!NO_BONE_LAYER.get(layer.getClass())) {
                    return null;
                }
            }
        }

        SESSION.renderer = renderer;
        SESSION.model = model;
        SESSION.poseStack = poseStack;
        SESSION.consumer = consumer;
        SESSION.builder = builder;
        SESSION.format = format;
        SESSION.light = light;
        SESSION.overlay = overlay;
        SESSION.red = red;
        SESSION.green = green;
        SESSION.blue = blue;
        SESSION.alpha = alpha;
        SESSION.recording = true;
        return SESSION;
    }

    private static boolean isEntityTranslucent(RenderType material) {
        var texture = RenderTypeUtils.getTextureLocation(material);
        return texture != null && (material == RenderType.entityTranslucent(texture, true)
                || material == RenderType.entityTranslucent(texture, false));
    }

    public static boolean capture(GeoRenderer<?> renderer, PoseStack stack, GeoBone bone, VertexConsumer buffer) {
        GeckoArModelBatch batch = SESSION;
        if (!batch.recording || batch.renderer != renderer || batch.poseStack != stack || batch.consumer != buffer) {
            return false;
        }
        if (bone.getCubes().isEmpty()) {
            return true;
        }
        PoseStack.Pose pose = stack.last();
        if (batch.size == MAX_BONES || !pose.pose().isFinite() || !pose.normal().isFinite()) {
            batch.replay();
            return false;
        }
        int index = batch.size++;
        batch.bones[index] = bone;
        batch.positions[index].set(pose.pose());
        batch.normals[index].set(pose.normal());
        return true;
    }

    public void submit() {
        if (!recording) {
            return;
        }
        recording = false;
        ICitadelMeshSource source = (ICitadelMeshSource) builder;
        ServerMesh mesh = size > 1 && source.frmc$canSubmitGpuPoses() ? findMesh() : null;
        if (mesh == null) {
            replay();
            return;
        }
        int color = FastColor.ARGB32.color((int) (alpha * 255.0F), (int) (red * 255.0F),
                (int) (green * 255.0F), (int) (blue * 255.0F));
        try {
            builder.beginTransform(positions[0], normals[0]);
            int first = source.frmc$activeGpuPose();
            for (int i = 1; i < size; i++) {
                builder.beginTransform(positions[i], normals[i]);
            }
            source.frmc$selectGpuPose(first);
            mesh.write(builder, color, light, overlay);
        } finally {
            builder.endTransform();
        }
    }

    private void replay() {
        recording = false;
        for (int i = 0; i < size; i++) {
            fallbackPose.last().pose().set(positions[i]);
            fallbackPose.last().normal().set(normals[i]);
            renderer.renderCubesOfBone(fallbackPose, bones[i], consumer, light, overlay, red, green, blue, alpha);
        }
    }

    public void clear() {
        Arrays.fill(bones, 0, size, null);
        size = 0;
        renderer = null;
        model = null;
        consumer = null;
        poseStack = null;
        builder = null;
        format = null;
        recording = false;
    }

    private ServerMesh findMesh() {
        List<Variant> variants = MESHES.get(model);
        if (variants != null) {
            for (Variant variant : variants) {
                if (variant.matches(bones, size, format)) {
                    return variant.mesh;
                }
            }
            if (variants.size() >= MAX_VARIANTS) {
                return null;
            }
        }
        if (!format.initialize()) {
            return null;
        }
        ServerMesh mesh = buildMesh();
        if (mesh != null) {
            GeoCube[][] cubes = new GeoCube[size][];
            for (int i = 0; i < size; i++) {
                cubes[i] = bones[i].getCubes().toArray(GeoCube[]::new);
            }
            MESHES.computeIfAbsent(model, ignored -> new ArrayList<>())
                    .add(new Variant(Arrays.copyOf(bones, size), cubes, format, mesh));
        }
        return mesh;
    }

    private ServerMesh buildMesh() {
        SimpleMeshCollector collector = new SimpleMeshCollector(builder.getLayout());
        IntArrayList tags = new IntArrayList();
        PoseStack cubePose = new PoseStack();
        Vector3f position = new Vector3f();
        Vector3f normal = new Vector3f();
        try (ByteBufferBuilder data = collector.getBuffer(); ByteBufferBuilder merged = new ByteBufferBuilder(4096)) {
            VertexConsumer writer = builder.decorate(collector);
            for (int i = 0; i < size; i++) {
                for (GeoCube cube : bones[i].getCubes()) {
                    cubePose.pushPose();
                    GeckoCubePose.rotate(cubePose, cube);
                    int tag = i | (flatNormalAxes(cube) << 16);
                    for (GeoQuad quad : cube.quads()) {
                        if (quad == null) {
                            continue;
                        }
                        if (quad.vertices().length != 4 || (long) (tags.size() + 1) * 4 * VERTEX_BYTES > MAX_BYTES) {
                            return null;
                        }
                        quad.normal().mul(cubePose.last().normal(), normal);
                        for (GeoVertex vertex : quad.vertices()) {
                            vertex.position().mulPosition(cubePose.last().pose(), position);
                            writer.vertex(position.x, position.y, position.z, 1, 1, 1, 1,
                                    vertex.texU(), vertex.texV(), 0, 0, normal.x, normal.y, normal.z);
                        }
                        tags.add(tag);
                    }
                    cubePose.popPose();
                }
            }
            collector.flush();
            int count = tags.size() * 4;
            long bytes = (long) count * VERTEX_BYTES;
            if (count == 0 || count != collector.getVertexCount()) {
                return null;
            }
            if (storage == null) {
                storage = new MeshBuffer(64L);
            }
            if (storage.getPosition() + bytes > MAX_BYTES || storage.overflow(bytes)) {
                return null;
            }
            try (ByteBufferBuilder.Result source = data.build()) {
                long address = MemoryUtil.memAddress(source.byteBuffer());
                for (int i = 0; i < tags.size(); i++) {
                    ArModelMeshUpload.appendVertices(merged, address + (long) i * 4 * format.vertexBytes,
                            4, format, tags.getInt(i));
                }
            }
            try (ByteBufferBuilder.Result result = merged.build()) {
                int offset = (int) (storage.append(MemoryUtil.memAddress(result.byteBuffer()), bytes) / VERTEX_BYTES);
                ServerMesh mesh = new ServerMesh(0, ServerMesh.Builder.COUNTER++, count, offset, true, storage);
                ArModelMeshUpload.register(mesh, format, null);
                return mesh;
            }
        }
    }

    // GeckoLib corrects the sign after the bone transform for cubes with a zero extent.
    private static int flatNormalAxes(GeoCube cube) {
        var size = cube.size();
        int axes = size.y == 0 || size.z == 0 ? 1 : 0;
        axes |= size.x == 0 || size.z == 0 ? 2 : 0;
        axes |= size.x == 0 || size.y == 0 ? 4 : 0;
        return axes;
    }

    public static void release() {
        MESHES.clear();
        if (storage != null) {
            storage.delete();
            storage = null;
        }
    }

    private record Variant(GeoBone[] bones, GeoCube[][] cubes, Format format, ServerMesh mesh) {
        boolean matches(GeoBone[] visible, int size, Format format) {
            if (this.format != format || bones.length != size) {
                return false;
            }
            for (int i = 0; i < size; i++) {
                List<GeoCube> current = visible[i].getCubes();
                if (bones[i] != visible[i] || cubes[i].length != current.size()) {
                    return false;
                }
                for (int cube = 0; cube < current.size(); cube++) {
                    if (cubes[i][cube] != current.get(cube)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
