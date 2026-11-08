package com.y271727uy.FRMC.integration.citadel.render.ar;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.core.buffers.memory.VertexLayout;
import com.github.argon4w.acceleratedrendering.core.meshes.IMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.MeshType;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.IMeshCollector;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.MeshCollectorType;
import com.github.argon4w.acceleratedrendering.core.meshes.data.MeshData;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.diagnostics.EntityRenderDiagnostics;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Keeps a small set of mesh variants per bone across AR builder lifetimes. */
public final class CitadelMeshCache {
    private static final int MAX_VARIANTS = 16;

    private final Entry[] entries = new Entry[MAX_VARIANTS];
    private final Map<MeshContent, IMesh> meshesByContent = new HashMap<>();
    private Object[] cubeIdentities = new Object[0];
    private int nextEntry;
    private WeakReference<IAcceleratedVertexConsumer> lastConsumer;
    private MeshCollectorType lastCollectorType;
    private MeshType lastMeshType;
    private int lastOverlay;
    private IMesh lastMesh;

    public void render(ObjectList<?> cubes, boolean advanced, IAcceleratedVertexConsumer consumer,
                       Matrix4f transform, Matrix3f normal, int light, int overlay, int color) {
        long partStart = EntityRenderDiagnostics.startCitadelPartRender();
        long transformStart = EntityRenderDiagnostics.start();
        consumer.beginTransform(transform, normal);
        try {
            EntityRenderDiagnostics.recordArBeginTransform(transformStart);
            IMesh mesh = getMesh(cubes, advanced, consumer, overlay);
            EntityRenderDiagnostics.recordMeshRecord();
            long submitStart = EntityRenderDiagnostics.start();
            mesh.write(consumer, color, light, overlay);
            EntityRenderDiagnostics.recordArMeshSubmission(submitStart);
        } finally {
            consumer.endTransform();
            EntityRenderDiagnostics.recordCitadelPartRender(partStart);
        }
    }

    IMesh getMesh(ObjectList<?> cubes, boolean advanced, IAcceleratedVertexConsumer consumer, int overlay) {
        if (!matchesCubes(cubes)) {
            cubeIdentities = cubes.toArray();
            Arrays.fill(entries, null);
            nextEntry = 0;
            lastMesh = null;
        }
        MeshCollectorType collectorType = CoreFeature.getMeshCollectorType();
        MeshType meshType = AcceleratedEntityRenderingFeature.getMeshType();
        // AR's culled collector always records NO_OVERLAY in the mesh.
        int meshOverlay = collectorType == MeshCollectorType.CULLED ? 0 : overlay;
        if (lastMesh != null && lastConsumer.get() == consumer && lastCollectorType == collectorType
                && lastMeshType == meshType && lastOverlay == meshOverlay) {
            EntityRenderDiagnostics.recordMeshCacheHit();
            return lastMesh;
        }

        ICitadelMeshSource source = consumer.getClass() == AcceleratedBufferBuilder.class
                && consumer instanceof ICitadelMeshSource meshSource ? meshSource : null;
        IMesh mesh = source == null ? null
                : getReusableMesh(cubes, advanced, consumer, source, collectorType, meshType, meshOverlay, overlay);
        if (mesh == null) {
            mesh = build(cubes, advanced, consumer, collectorType, meshType, overlay);
        }
        if (lastConsumer == null || lastConsumer.get() != consumer) {
            lastConsumer = source == null ? new WeakReference<>(consumer) : source.frmc$meshConsumerReference();
        }
        lastCollectorType = collectorType;
        lastMeshType = meshType;
        lastOverlay = meshOverlay;
        lastMesh = mesh;
        return mesh;
    }

    private IMesh getReusableMesh(ObjectList<?> cubes, boolean advanced, IAcceleratedVertexConsumer consumer,
                                  ICitadelMeshSource source, MeshCollectorType collectorType, MeshType meshType,
                                  int meshOverlay, int overlay) {
        boolean culled = collectorType == MeshCollectorType.CULLED;
        NativeImage texture = culled ? source.frmc$meshTexture() : null;
        if (culled && texture == null) {
            return null;
        }
        VertexLayout layout = consumer.getLayout();
        int polygonSize = consumer.getPolygonSize();
        for (Entry entry : entries) {
            if (entry == null) {
                break;
            }
            if (entry.matches(layout, polygonSize, collectorType, meshType, meshOverlay, texture)) {
                EntityRenderDiagnostics.recordMeshCacheHit();
                return entry.mesh();
            }
        }

        // The texture may have been evicted from AR's image cache since this builder first used it.
        boolean reusable = !culled || consumer.downloadTexture() == texture;
        IMesh mesh = build(cubes, advanced, consumer, collectorType, meshType, overlay);
        if (reusable) {
            entries[nextEntry] = new Entry(layout, polygonSize, collectorType, meshType, meshOverlay,
                    culled ? new WeakReference<>(texture) : null, mesh);
            nextEntry = (nextEntry + 1) % entries.length;
        }
        return mesh;
    }

    private boolean matchesCubes(ObjectList<?> cubes) {
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

    private IMesh build(ObjectList<?> cubes, boolean advanced, IAcceleratedVertexConsumer consumer,
                        MeshCollectorType collectorType, MeshType meshType, int overlay) {
        EntityRenderDiagnostics.recordMeshCacheBuild();
        IMeshCollector collector = collectorType.create(consumer);
        VertexConsumer writer = consumer.decorate(collector);
        if (advanced) {
            CitadelArRenderer.collectTabula(cubes, writer, overlay);
        } else {
            CitadelArRenderer.collectBasic(cubes, writer, overlay);
        }
        collector.flush();
        // Retain the original per-part deduplication for consumers that cannot share a stable entry.
        MeshContent content = new MeshContent(meshType, collector.getData());
        IMesh mesh = meshesByContent.get(content);
        if (mesh != null) {
            collector.getBuffer().close();
            return mesh;
        }
        mesh = meshType.getBuilder().build(collector);
        meshesByContent.put(content, mesh);
        return mesh;
    }

    private record MeshContent(MeshType type, MeshData data) {}

    private record Entry(VertexLayout layout, int polygonSize, MeshCollectorType collectorType,
                         MeshType meshType, int overlay, WeakReference<NativeImage> texture, IMesh mesh) {
        private boolean matches(VertexLayout layout, int polygonSize, MeshCollectorType collectorType,
                                MeshType meshType, int overlay, NativeImage texture) {
            return this.layout == layout && this.polygonSize == polygonSize && this.collectorType == collectorType
                    && this.meshType == meshType && this.overlay == overlay
                    && (this.texture == null || this.texture.get() == texture);
        }
    }
}
