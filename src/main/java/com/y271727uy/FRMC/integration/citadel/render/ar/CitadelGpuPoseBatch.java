package com.y271727uy.FRMC.integration.citadel.render.ar;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.meshes.MeshType;
import com.github.argon4w.acceleratedrendering.core.meshes.ServerMesh;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.config.Config;
import com.y271727uy.FRMC.integration.acceleratedrendering.ArModelMeshUpload;
import com.y271727uy.FRMC.integration.alexscaves.HullbreakerRenderMaterials;
import com.y271727uy.FRMC.integration.citadel.model.CitadelGeometrySelection;
import com.y271727uy.FRMC.integration.citadel.model.CitadelSkeletonLayoutCache;
import com.y271727uy.FRMC.integration.citadel.render.ar.CitadelGpuPoseProgram.BufferBinding;
import com.y271727uy.FRMC.integration.oculus.OculusIntegration;
import com.y271727uy.FRMC.integration.untamedwilds.client.render.CitadelBoarLod;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;
import net.irisshaders.iris.api.v0.IrisApi;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;

import static org.lwjgl.opengl.GL45C.*;

/** Queues Citadel poses for the AR sharing buffer, then evaluates them in one GPU dispatch. */
public final class CitadelGpuPoseBatch {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int RECORD_BYTES = 64;
    private static final int MAX_BONES = 64;
    private static final int MAX_BATCH = 4096;
    private static final int MAX_REPORTED_MODELS = 32;
    private static final Set<Class<?>> REPORTED_GENERAL_MODELS = new HashSet<>();
    private static final Set<Class<?>> REPORTED_GENERAL_LOD = new HashSet<>();
    private static boolean reportedMaterialBatch;
    private static boolean reportedLod;

    private ByteBuffer input = BufferUtils.createByteBuffer(RECORD_BYTES * 64);
    private IntBuffer offsets = BufferUtils.createIntBuffer(64);
    private final CitadelModelLod modelLod = new CitadelModelLod();
    private int program;
    private int inputBuffer;
    private int offsetBuffer;
    private boolean unavailable;
    private int materialSubmissions;
    private int replacedBoneSubmissions;
    private int boarMeshes;
    private int reducedMeshes;
    private int materialVertices;
    private int savedVertices;

    public static boolean render(AdvancedModelBox root, PoseStack.Pose base,
                                 AcceleratedBufferBuilder builder, int light, int overlay, int color) {
        if (!root.showModel || !RenderSystem.isOnRenderThread()
                || AcceleratedEntityRenderingFeature.getMeshType() != MeshType.SERVER
                || !(builder instanceof ICitadelMeshSource source) || !source.frmc$canSubmitGpuPoses()
                || !(builder.getBuffer() instanceof ICitadelGpuBuffers buffers)) {
            return false;
        }
        Object model = root.getModel();
        if (model == null) {
            return false;
        }
        // Keep the previously supported per-bone path and optional LOD isolated from general models.
        String modelType = model.getClass().getName();
        boolean boar = modelType.equals("untamedwilds.client.model.ModelBoar")
                || modelType.equals("untamedwilds.client.model.ModelBoarPiglet");
        boolean hullbreaker = modelType.equals("com.github.alexmodguy.alexscaves.client.model.HullbreakerModel")
                && CoreFeature.shouldForceAccelerateTranslucent()
                && (!OculusIntegration.IS_IRIS_INSTALLED || !IrisApi.getInstance().isShaderPackInUse())
                && HullbreakerRenderMaterials.isBodyOrGlow(ArModelMeshUpload.unwrap(builder.getRenderType()));
        if (!boar && (!Config.citadelGpuModelBatchingEnabled
                || root.getClass() != AdvancedModelBox.class
                || (!hullbreaker && (!Config.citadelGpuGeneralModelsEnabled
                        || !CitadelGpuMaterialMeshes.isVanillaCutout(builder.getRenderType()))))) {
            return false;
        }
        CitadelGpuPoseBatch batch = buffers.frmc$citadelPoseBatch();
        return batch.submit(root, base, builder, source, buffers, light, overlay, color, model, boar, hullbreaker);
    }

    private boolean submit(AdvancedModelBox root, PoseStack.Pose base, AcceleratedBufferBuilder builder,
                           ICitadelMeshSource source, ICitadelGpuBuffers buffers, int light, int overlay, int color,
                           Object model, boolean boar, boolean allowTranslucent) {
        if (offsets.position() >= MAX_BATCH || unavailable) {
            return false;
        }
        CitadelSkeletonLayoutCache.Layout layout = CitadelSkeletonLayoutCache.getOrBuild(root);
        BasicModelPart[] nodes = layout.nodes();
        int[] parents = layout.parentIndices();
        int[] depths = layout.depths();
        int[] ends = layout.topology().subtreeEnds();
        if (nodes.length > MAX_BONES || !checkTree(nodes, parents, ends)) {
            return false;
        }
        long visibleGeometry = CitadelGeometrySelection.visibleGeometry(layout);
        int visibleParts = Long.bitCount(visibleGeometry);
        if (visibleParts == 0 || (!boar && (visibleParts < 2 || !base.pose().isFinite() || !base.normal().isFinite()))
                || !initialize()) {
            return false;
        }

        ServerMesh modelMesh = null;
        int saved = 0;
        boolean reduced = false;
        boolean diagnostics = Config.entityRenderDiagnosticsEnabled;
        boolean sampleLod = diagnostics && boar && Config.citadelBoarLodEnabled && !reportedLod;
        if (Config.citadelGpuModelBatchingEnabled) {
            long geometry = visibleGeometry;
            if (boar && Config.citadelBoarLodEnabled && CitadelGpuMaterialMeshes.isVanillaCutout(builder.getRenderType())) {
                long omittedParts = CitadelBoarLod.omittedParts(model, nodes, base.pose());
                geometry = CitadelGeometrySelection.omitOwnGeometry(visibleGeometry, omittedParts);
            }
            if (geometry != visibleGeometry) {
                modelMesh = CitadelGpuMaterialMeshes.get(root, nodes, geometry, builder, overlay, boar, allowTranslucent);
                reduced = modelMesh != null;
                if (reduced && sampleLod) {
                    ServerMesh full = CitadelGpuMaterialMeshes.get(root, nodes, visibleGeometry, builder, overlay, boar, allowTranslucent);
                    if (full != null) {
                        saved = full.size() - modelMesh.size();
                    }
                }
            }
            if (modelMesh == null) {
                modelMesh = CitadelGpuMaterialMeshes.get(root, nodes, visibleGeometry, builder, overlay, boar, allowTranslucent);
            }
            if (modelMesh != null && Config.citadelGeneralLodEnabled && (!boar || !Config.citadelBoarLodEnabled)
                    && CitadelModelLod.activeFor(model)) {
                var lod = CitadelGpuMaterialMeshes.lodVariant(modelMesh);
                if (lod != null && modelLod.canReduce(lod.plan, nodes, parents, base.pose())) {
                    long detailGeometry = CitadelGeometrySelection.omitOwnGeometry(visibleGeometry, lod.plan.omitted());
                    ServerMesh detailMesh = lod.get(nodes, detailGeometry, builder, overlay);
                    if (detailMesh != null && detailMesh.size() < modelMesh.size()) {
                        if (diagnostics && REPORTED_GENERAL_LOD.size() < MAX_REPORTED_MODELS
                                && REPORTED_GENERAL_LOD.add(model.getClass())) {
                            LOGGER.info("[FRMC/CitadelGeneralLOD] model={} fullVertices={} reducedVertices={}",
                                    model.getClass().getName(), modelMesh.size(), detailMesh.size());
                        }
                        modelMesh = detailMesh;
                    }
                }
            }
        }
        // General models enter only when the material can be batched. Their original renderer owns all fallbacks.
        if (!boar && modelMesh == null) {
            return false;
        }
        // Mesh selection leaves every bone in the pose batch so children inherit omitted parents' transforms.
        ensureCapacity((nodes.length + 2) * RECORD_BYTES);
        int firstSharing = buffers.frmc$reserveCitadelPoses(nodes.length);
        pack(base, firstSharing, layout.levelStarts().length - 2, nodes, parents, depths);
        try {
            if (modelMesh != null) {
                source.frmc$selectGpuPose(firstSharing);
                modelMesh.write(builder, color, light, overlay);
                if (diagnostics && !reportedMaterialBatch) {
                    materialSubmissions++;
                    replacedBoneSubmissions += visibleParts;
                }
                if (diagnostics && !boar && REPORTED_GENERAL_MODELS.size() < MAX_REPORTED_MODELS
                        && REPORTED_GENERAL_MODELS.add(model.getClass())) {
                    LOGGER.info("[FRMC/CitadelGeneralGpu] model={} nodes={} visibleParts={} vertices={}",
                            model.getClass().getName(), nodes.length, visibleParts, modelMesh.size());
                }
                if (sampleLod) {
                    boarMeshes++;
                    materialVertices += modelMesh.size();
                    if (reduced) {
                        reducedMeshes++;
                        savedVertices += saved;
                    }
                }
                return true;
            }
            for (int i = 0; i < nodes.length; i++) {
                AdvancedModelBox node = (AdvancedModelBox) nodes[i];
                if ((visibleGeometry & (1L << i)) != 0) {
                    source.frmc$selectGpuPose(firstSharing + i);
                    ((ICitadelGpuPart) node).frmc$citadelMeshCache().getMesh(node.cubeList, true, builder, overlay)
                            .write(builder, color, light, overlay);
                }
            }
        } finally {
            builder.endTransform();
        }
        return true;
    }

    private static boolean checkTree(BasicModelPart[] nodes, int[] parents, int[] ends) {
        if (parents[0] != -1) {
            return false;
        }
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].getClass() != AdvancedModelBox.class || !(nodes[i] instanceof ICitadelGpuPart)) {
                return false;
            }
            AdvancedModelBox node = (AdvancedModelBox) nodes[i];
            if (!Float.isFinite(node.rotationPointX) || !Float.isFinite(node.rotationPointY)
                    || !Float.isFinite(node.rotationPointZ) || !Float.isFinite(node.rotateAngleX)
                    || !Float.isFinite(node.rotateAngleY) || !Float.isFinite(node.rotateAngleZ)
                    || !supportedScale(node.scaleX) || !supportedScale(node.scaleY) || !supportedScale(node.scaleZ)) {
                return false;
            }
            ObjectList<BasicModelPart> children = node.childModels;
            int next = i + 1;
            for (int child = 0; child < children.size(); child++) {
                if (next >= ends[i] || nodes[next] != children.get(child) || parents[next] != i) {
                    return false;
                }
                next = ends[next];
            }
            if (next != ends[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean supportedScale(float scale) {
        return scale >= 0.0001F && scale <= 10000.0F;
    }

    private void pack(PoseStack.Pose base, int firstSharing, int depth,
                      BasicModelPart[] nodes, int[] parents, int[] depths) {
        int start = input.position();
        offsets.put(start / RECORD_BYTES);
        base.pose().get(start, input);
        input.position(start + RECORD_BYTES);
        var normal = base.normal();
        putVector(normal.m00(), normal.m01(), normal.m02());
        putVector(normal.m10(), normal.m11(), normal.m12());
        putVector(normal.m20(), normal.m21(), normal.m22());
        input.putInt(nodes.length).putInt(depth).putInt(firstSharing).putInt(0);
        for (int i = 0; i < nodes.length; i++) {
            AdvancedModelBox node = (AdvancedModelBox) nodes[i];
            putVector(node.rotationPointX / 16.0F, node.rotationPointY / 16.0F, node.rotationPointZ / 16.0F);
            putVector(node.rotateAngleX, node.rotateAngleY, node.rotateAngleZ);
            putVector(node.scaleX, node.scaleY, node.scaleZ);
            input.putInt(parents[i]).putInt(depths[i]).putInt(node.scaleChildren ? 1 : 0).putInt(0);
        }
    }

    private void putVector(float x, float y, float z) {
        input.putFloat(x).putFloat(y).putFloat(z).putFloat(0);
    }

    private void ensureCapacity(int bytes) {
        int required = input.position() + bytes;
        if (required > input.capacity()) {
            input = BufferUtils.createByteBuffer(Math.max(required, input.capacity() * 2)).put(input.flip());
        }
        if (!offsets.hasRemaining()) {
            offsets = BufferUtils.createIntBuffer(offsets.capacity() * 2).put(offsets.flip());
        }
    }

    private boolean initialize() {
        if (!unavailable && program == 0) {
            try {
                if (!GL.getCapabilities().OpenGL45) {
                    unavailable = true;
                    return false;
                }
                program = CitadelGpuPoseProgram.compileProgram();
                inputBuffer = glCreateBuffers();
                offsetBuffer = glCreateBuffers();
            } catch (IOException | RuntimeException exception) {
                unavailable = true;
                close();
                LOGGER.warn("[FRMC/CitadelGpuRender] GPU pose program could not start", exception);
            }
        }
        return !unavailable;
    }

    public void flush(int sharingBuffer) {
        int roots = offsets.position();
        if (roots == 0) {
            return;
        }
        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int previousBuffer = glGetInteger(GL_SHADER_STORAGE_BUFFER_BINDING);
        BufferBinding previousInput = BufferBinding.capture(0);
        BufferBinding previousOutput = BufferBinding.capture(1);
        BufferBinding previousOffsets = BufferBinding.capture(2);
        try {
            glNamedBufferData(inputBuffer, input.flip(), GL_STREAM_DRAW);
            glNamedBufferData(offsetBuffer, offsets.flip(), GL_STREAM_DRAW);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, inputBuffer);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, sharingBuffer);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, offsetBuffer);
            glUseProgram(program);
            glUniform1i(2, 1);
            glDispatchCompute(roots, 1, 1);
            glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT | GL_BUFFER_UPDATE_BARRIER_BIT);
            if (Config.entityRenderDiagnosticsEnabled && !reportedMaterialBatch && materialSubmissions > 0) {
                LOGGER.info("[FRMC/CitadelGpuRender] Queued {} material meshes in place of {} bone submissions",
                        materialSubmissions, replacedBoneSubmissions);
                reportedMaterialBatch = true;
            }
            if (Config.entityRenderDiagnosticsEnabled && !reportedLod && reducedMeshes > 0) {
                LOGGER.info("[FRMC/CitadelBoarLOD] fullMeshes={} reducedMeshes={} submittedVertices={} savedVertices={}",
                        boarMeshes - reducedMeshes, reducedMeshes, materialVertices, savedVertices);
                reportedLod = true;
            }
        } finally {
            glUseProgram(previousProgram);
            previousInput.restore(0);
            previousOutput.restore(1);
            previousOffsets.restore(2);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, previousBuffer);
            clear();
        }
    }

    public void clear() {
        input.clear();
        offsets.clear();
        materialSubmissions = 0;
        replacedBoneSubmissions = 0;
        boarMeshes = 0;
        reducedMeshes = 0;
        materialVertices = 0;
        savedVertices = 0;
    }

    public void close() {
        glDeleteBuffers(inputBuffer);
        glDeleteBuffers(offsetBuffer);
        if (program != 0) {
            glDeleteProgram(program);
        }
        inputBuffer = 0;
        offsetBuffer = 0;
        program = 0;
        clear();
    }
}
