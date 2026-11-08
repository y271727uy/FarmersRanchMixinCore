package com.y271727uy.FRMC.integration.citadel.render.ar;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.buffers.memory.VertexLayout;
import com.github.argon4w.acceleratedrendering.core.meshes.MeshBuffer;
import com.github.argon4w.acceleratedrendering.core.meshes.ServerMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.IMeshCollector;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.MeshCollectorType;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.TransformProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.meshes.MeshUploadingProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.overrides.ProgramOverride;
import com.github.argon4w.acceleratedrendering.core.utils.ByteBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.utils.RenderTypeUtils;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.config.Config;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import static org.lwjgl.opengl.GL45C.*;

/** Builds one immutable, bone-indexed source mesh for a Citadel model and material. */
public final class CitadelGpuMaterialMeshes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int VERTEX_BYTES = 36;
    private static final int OVERLAY_OFFSET = 24;
    private static final int MESH_INFO_BYTES = 28;
    private static final int VARYING_BYTES = 16;
    private static final int PART_BYTES = 32;
    private static final int MAX_VARIANTS = 16;
    private static final long MAX_MESH_BYTES = 8L * 1024 * 1024;
    private static final long MAX_LOD_BYTES = 2L * 1024 * 1024;
    private static final Map<AdvancedModelBox, List<Variant>> MESHES = new WeakHashMap<>();
    private static final Map<ServerMesh, MeshDetails> MESH_DETAILS = new IdentityHashMap<>();
    private static MeshBuffer storage;
    private static MeshBuffer lodStorage;
    private static int program;
    private static boolean unavailable;
    private static boolean hookActive;

    private CitadelGpuMaterialMeshes() {
    }

    static ServerMesh get(AdvancedModelBox root, BasicModelPart[] nodes, long visible,
                          AcceleratedBufferBuilder builder, int overlay, boolean boar) {
        RenderType material = builder.getRenderType();
        ProgramOverride override = builder.getProgramOverride();
        if (!hookActive || unavailable || builder.getMode() != VertexFormat.Mode.QUADS
                || material.format() != DefaultVertexFormat.NEW_ENTITY
                || RenderTypeUtils.isTranslucent(material)
                || builder.getVertexSize() != VERTEX_BYTES
                || override.overrideId() != 0
                || override.getMeshInfoSize() != MESH_INFO_BYTES || override.getVaryingSize() != VARYING_BYTES
                || override.uploading().getClass() != MeshUploadingProgramDispatcher.Default.class
                || override.transform().getClass() != TransformProgramDispatcher.Default.class) {
            return null;
        }
        MeshCollectorType collector = CoreFeature.getMeshCollectorType();
        // AR can evict an alpha image while a builder still exists.
        NativeImage texture = collector == MeshCollectorType.CULLED ? builder.downloadTexture() : null;
        if (collector == MeshCollectorType.CULLED && texture == null) {
            return null;
        }
        VertexLayout layout = builder.getLayout();
        boolean depthTested = CitadelGeometryVisibility.cullingEnabled(boar) && isVanillaCutout(material);
        List<Variant> variants = MESHES.computeIfAbsent(root, ignored -> new ArrayList<>());
        for (Variant variant : variants) {
            if (variant.matches(material, override, layout, collector, texture, depthTested, boar, visible, nodes)) {
                return variant.mesh();
            }
        }
        if (variants.size() >= MAX_VARIANTS || !initialize()) {
            return null;
        }
        boolean planLod = Config.citadelGeneralLodEnabled && (!boar || !Config.citadelBoarLodEnabled)
                && isVanillaCutout(material);
        ServerMesh mesh = build(nodes, visible, builder, collector, overlay, depthTested, boar, planLod,
                storage, MAX_MESH_BYTES);
        if (mesh != null) {
            Object[][] cubes = new Object[nodes.length][];
            for (int i = 0; i < nodes.length; i++) {
                if ((visible & (1L << i)) != 0) {
                    cubes[i] = ((AdvancedModelBox) nodes[i]).cubeList.toArray();
                }
            }
            variants.add(new Variant(material, override, layout, collector,
                    texture == null ? null : new WeakReference<>(texture), depthTested, boar, visible, cubes, mesh));
        }
        return mesh;
    }

    static LodVariant lodVariant(ServerMesh mesh) {
        MeshDetails details = MESH_DETAILS.get(mesh);
        return details == null ? null : details.lod();
    }

    static boolean isVanillaCutout(RenderType material) {
        ResourceLocation texture = RenderTypeUtils.getTextureLocation(material);
        return texture != null && (material == RenderType.entityCutout(texture)
                || material == RenderType.entityCutoutNoCull(texture, true)
                || material == RenderType.entityCutoutNoCull(texture, false));
    }

    private static boolean initialize() {
        if (program == 0) {
            try {
                program = CitadelGpuPoseProgram.compileProgram("citadel_model_upload.comp");
                storage = new MeshBuffer(64L);
            } catch (IOException | RuntimeException exception) {
                unavailable = true;
                release();
                LOGGER.warn("[FRMC/CitadelGpuRender] Whole-model upload unavailable", exception);
                return false;
            }
        }
        return true;
    }

    private static ServerMesh build(BasicModelPart[] nodes, long visible, AcceleratedBufferBuilder builder,
                                    MeshCollectorType collectorType, int overlay, boolean depthTested, boolean boar,
                                    boolean planLod, MeshBuffer destinationStorage, long maxBytes) {
        try (ByteBufferBuilder merged = new ByteBufferBuilder(4096); MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer bounds = depthTested ? stack.malloc(nodes.length * PART_BYTES) : null;
            var lodBounds = planLod ? new CitadelModelLod.MeshBounds(nodes.length) : null;
            int vertices = 0;
            for (int bone = 0; bone < nodes.length; bone++) {
                ObjectList<?> cubes = ((AdvancedModelBox) nodes[bone]).cubeList;
                if ((visible & (1L << bone)) == 0 || cubes.isEmpty()) {
                    continue;
                }
                IMeshCollector collector = collectorType.create(builder);
                try (ByteBufferBuilder data = collector.getBuffer()) {
                    CitadelArRenderer.collectTabula(cubes, builder.decorate(collector), overlay);
                    collector.flush();
                    int count = Math.toIntExact(collector.getVertexCount());
                    if (count == 0) {
                        continue;
                    }
                    if (count % 4 != 0) {
                        return null;
                    }
                    int bytes = count * VERTEX_BYTES;
                    try (ByteBufferBuilder.Result result = data.build()) {
                        long destination = merged.reserve(bytes);
                        MemoryUtil.memCopy(MemoryUtil.memAddress(result.byteBuffer()), destination, bytes);
                        if (bounds != null || lodBounds != null) {
                            putPartBounds(bounds, lodBounds, bone, destination, vertices, count);
                        }
                        for (int vertex = 0; vertex < count; vertex++) {
                            MemoryUtil.memPutInt(destination + (long) vertex * VERTEX_BYTES + OVERLAY_OFFSET, bone);
                        }
                    }
                    vertices += count;
                }
            }
            long bytes = (long) vertices * VERTEX_BYTES;
            if (vertices == 0 || destinationStorage.getPosition() + bytes > maxBytes
                    || destinationStorage.overflow(bytes)) {
                return null;
            }
            try (ByteBufferBuilder.Result result = merged.build()) {
                int offset = (int) (destinationStorage.append(MemoryUtil.memAddress(result.byteBuffer()), bytes) / VERTEX_BYTES);
                ServerMesh mesh = new ServerMesh(0, ServerMesh.Builder.COUNTER++, vertices, offset, true, destinationStorage);
                PartBounds partBounds = null;
                if (bounds != null) {
                    int partCount = bounds.position() / PART_BYTES;
                    int buffer = glCreateBuffers();
                    glNamedBufferStorage(buffer, bounds.flip(), 0);
                    partBounds = new PartBounds(buffer, partCount);
                }
                var plan = lodBounds == null ? null : lodBounds.plan(vertices);
                var lod = plan == null ? null : new LodVariant(plan, collectorType, depthTested, boar);
                MESH_DETAILS.put(mesh, new MeshDetails(boar, partBounds, lod));
                return mesh;
            }
        }
    }

    private static void putPartBounds(ByteBuffer bounds, CitadelModelLod.MeshBounds lodBounds,
                                      int bone, long source, int first, int count) {
        float minX = Float.POSITIVE_INFINITY, minY = minX, minZ = minX;
        float maxX = Float.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        for (int i = 0; i < count; i++) {
            long address = source + (long) i * VERTEX_BYTES;
            float x = MemoryUtil.memGetFloat(address);
            float y = MemoryUtil.memGetFloat(address + Float.BYTES);
            float z = MemoryUtil.memGetFloat(address + 2 * Float.BYTES);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        if (bounds != null) {
            bounds.putFloat(minX).putFloat(minY).putFloat(minZ).putInt(first);
            bounds.putFloat(maxX).putFloat(maxY).putFloat(maxZ).putInt(count);
        }
        if (lodBounds != null) {
            lodBounds.add(bone, count, minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    /** Called only from AR's dense upload, after it binds its mesh and instance SSBOs. */
    public static boolean handles(ServerMesh mesh) {
        hookActive = true;
        return mesh != null && (storage != null && mesh.meshBuffer() == storage
                || lodStorage != null && mesh.meshBuffer() == lodStorage);
    }

    public static int dispatch(ServerMesh mesh, int count, int vertices, int offset) {
        int previous = glGetInteger(GL_CURRENT_PROGRAM);
        try {
            glUseProgram(program);
            glUniform1ui(0, count);
            glUniform1ui(1, vertices);
            glUniform1ui(2, offset);
            glDispatchCompute((count * vertices + 127) / 128, 1, 1);
            MeshDetails details = MESH_DETAILS.get(mesh);
            boolean culled = details != null && CitadelGeometryVisibility.dispatch(
                    count, vertices, offset, details.bounds(), details.boar());
            return GL_SHADER_STORAGE_BARRIER_BIT | (culled ? GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT : 0);
        } finally {
            glUseProgram(previous);
        }
    }

    public static void release() {
        CitadelGeometryVisibility.release();
        MESHES.clear();
        for (MeshDetails details : MESH_DETAILS.values()) {
            if (details.bounds() != null) {
                glDeleteBuffers(details.bounds().buffer());
            }
        }
        MESH_DETAILS.clear();
        if (storage != null) {
            storage.delete();
            storage = null;
        }
        if (lodStorage != null) {
            lodStorage.delete();
            lodStorage = null;
        }
        if (program != 0) {
            glDeleteProgram(program);
            program = 0;
        }
    }

    record PartBounds(int buffer, int count) {
    }

    private record MeshDetails(boolean boar, PartBounds bounds, LodVariant lod) {
    }

    static final class LodVariant {
        final CitadelModelLod.Plan plan;
        private final MeshCollectorType collector;
        private final boolean depthTested;
        private final boolean boar;
        private boolean attempted;
        private ServerMesh mesh;

        private LodVariant(CitadelModelLod.Plan plan, MeshCollectorType collector, boolean depthTested, boolean boar) {
            this.plan = plan;
            this.collector = collector;
            this.depthTested = depthTested;
            this.boar = boar;
        }

        ServerMesh get(BasicModelPart[] nodes, long visible, AcceleratedBufferBuilder builder, int overlay) {
            if (!attempted) {
                attempted = true;
                if (lodStorage == null) {
                    lodStorage = new MeshBuffer(64L);
                }
                if (lodStorage.getPosition() + (long) plan.vertices() * VERTEX_BYTES <= MAX_LOD_BYTES) {
                    mesh = build(nodes, visible, builder, collector, overlay, depthTested, boar, false,
                            lodStorage, MAX_LOD_BYTES);
                }
            }
            return mesh;
        }
    }

    private record Variant(RenderType material, ProgramOverride override, VertexLayout layout,
                           MeshCollectorType collector, WeakReference<NativeImage> texture,
                           boolean depthTested, boolean boar, long visible, Object[][] cubes, ServerMesh mesh) {
        private boolean matches(RenderType material, ProgramOverride override, VertexLayout layout,
                                MeshCollectorType collector, NativeImage texture, boolean depthTested,
                                boolean boar, long visible, BasicModelPart[] nodes) {
            if (this.material != material || this.override != override || this.layout != layout
                    || this.collector != collector || this.depthTested != depthTested
                    || this.boar != boar || this.visible != visible
                    || cubes.length != nodes.length || (this.texture != null && this.texture.get() != texture)) {
                return false;
            }
            for (int i = 0; i < nodes.length; i++) {
                Object[] saved = cubes[i];
                if (saved == null) {
                    continue;
                }
                ObjectList<?> current = ((AdvancedModelBox) nodes[i]).cubeList;
                if (saved.length != current.size()) {
                    return false;
                }
                for (int cube = 0; cube < saved.length; cube++) {
                    if (saved[cube] != current.get(cube)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
