package com.y271727uy.FRMC.integration.citadel.render.ar;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.draw.basevertex.BaseVertexDrawMethod;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.config.Config;
import com.y271727uy.FRMC.integration.citadel.render.ar.CitadelGpuMaterialMeshes.PartBounds;
import com.y271727uy.FRMC.integration.citadel.render.ar.CitadelGpuPoseProgram.BufferBinding;
import com.y271727uy.FRMC.integration.oculus.OculusIntegration;
import java.io.IOException;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

import static org.lwjgl.opengl.GL45C.*;

/** Uses a same-frame terrain depth snapshot to measure and cull supported Citadel geometry. */
public final class CitadelGeometryVisibility {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_CAPTURE_ATTEMPTS = 3;
    private static final int MAX_INSTANCES = 64;
    private static final int MAX_CULL_INSTANCES = 4096;
    private static final int MAX_MESH_VERTICES = 2048;
    private static final int COUNTER_BINDING = 0;
    private static final int COUNTER_COUNT = 10;
    private static final int PART_BINDING = 3;
    private static final int DEPTH_UNIT = 0;
    private static final Matrix4f projection = new Matrix4f();
    private static final Matrix4f modelView = new Matrix4f();
    private static boolean armed;
    private static boolean cullingArmed;
    private static boolean captured;
    private static boolean collecting;
    private static boolean culling;
    private static boolean probeComplete;
    private static boolean unavailable;
    private static boolean warnedTarget;
    private static ShaderInstance cutoutShader;
    private static ShaderInstance cutoutNoCullShader;
    private static boolean stockShaders;
    private static int snapshot;
    private static int counters;
    private static int program;
    private static int width;
    private static int height;
    private static int framebuffer;
    private static int depthAttachment;
    private static int captureAttempts;
    private static int sampledInstances;
    private static int sampledQuads;
    private static int sampledParts;

    private CitadelGeometryVisibility() {
    }

    public static void beginFrame() {
        captured = false;
        collecting = false;
        culling = false;
        sampledInstances = 0;
        sampledQuads = 0;
        sampledParts = 0;
    }

    public static void captureTerrainDepth(Matrix4f terrainProjection) {
        boolean requestProbe = armed && Config.citadelBottomFaceProbeEnabled && !probeComplete;
        boolean requestCull = cullingArmed && cullingEnabled();
        cullingArmed = false;
        if (unavailable || (!requestProbe && !requestCull)) {
            return;
        }
        if (OculusIntegration.IS_IRIS_INSTALLED && IrisApi.getInstance().isShaderPackInUse()) {
            warnTarget("shader pack active");
            return;
        }
        if (requestCull && !stockEntityShaders()) {
            warnTarget("entity cutout shader replaced");
            requestCull = false;
            if (!requestProbe) {
                return;
            }
        }
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (!supportedTarget(target)) {
            warnTarget("terrain depth target or viewport is unsupported");
            return;
        }
        try {
            if (program == 0) {
                program = CitadelGpuPoseProgram.compileProgram("citadel_bottom_face_probe.comp");
                counters = glCreateBuffers();
                glNamedBufferStorage(counters, COUNTER_COUNT * (long) Integer.BYTES, 0);
            }
            if (snapshot == 0 || width != target.width || height != target.height) {
                if (snapshot != 0) {
                    glDeleteTextures(snapshot);
                }
                width = target.width;
                height = target.height;
                snapshot = glCreateTextures(GL_TEXTURE_2D);
                glTextureStorage2D(snapshot, 1, GL_DEPTH_COMPONENT32F, width, height);
                glTextureParameteri(snapshot, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTextureParameteri(snapshot, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            }
            // Keep untouched texels at far depth if the copy is rejected by the driver.
            glClearTexImage(snapshot, 0, GL_DEPTH_COMPONENT, GL_FLOAT, new float[]{1.0f});
            glCopyTextureSubImage2D(snapshot, 0, 0, 0, 0, 0, width, height);
            if (requestProbe) {
                glClearNamedBufferData(counters, GL_R32UI, GL_RED_INTEGER, GL_UNSIGNED_INT, (int[]) null);
                captureAttempts++;
            }
            projection.set(terrainProjection);
            modelView.set(RenderSystem.getModelViewMatrix());
            framebuffer = target.frameBufferId;
            depthAttachment = target.getDepthTextureId();
            collecting = requestProbe;
            culling = requestCull;
            captured = true;
        } catch (IOException | RuntimeException exception) {
            unavailable = true;
            release();
            LOGGER.warn("[FRMC/CitadelGeometryVisibility] Unavailable", exception);
        }
    }

    private static boolean stockEntityShaders() {
        ShaderInstance cutout = GameRenderer.getRendertypeEntityCutoutShader();
        ShaderInstance noCull = GameRenderer.getRendertypeEntityCutoutNoCullShader();
        if (cutout == null || noCull == null) {
            return false;
        }
        if (cutout != cutoutShader || noCull != cutoutNoCullShader) {
            ResourceManager resources = Minecraft.getInstance().getResourceManager();
            stockShaders = stockShader(resources, "rendertype_entity_cutout")
                    && stockShader(resources, "rendertype_entity_cutout_no_cull")
                    && stockResource(resources, "shaders/include/fog.glsl")
                    && stockResource(resources, "shaders/include/light.glsl");
            cutoutShader = cutout;
            cutoutNoCullShader = noCull;
        }
        return stockShaders;
    }

    private static boolean stockShader(ResourceManager resources, String shader) {
        for (String extension : new String[]{".json", ".vsh", ".fsh"}) {
            if (!stockResource(resources, "shaders/core/" + shader + extension)) {
                return false;
            }
        }
        return true;
    }

    private static boolean stockResource(ResourceManager resources, String path) {
        return resources.getResource(new ResourceLocation("minecraft", path))
                .filter(resource -> "vanilla".equals(resource.sourcePackId())).isPresent();
    }

    private static boolean supportedTarget(RenderTarget target) {
        if (target.getDepthTextureId() <= 0 || target.frameBufferId <= 0
                || target.viewWidth != target.width || target.viewHeight != target.height
                || glGetInteger(GL_READ_FRAMEBUFFER_BINDING) != target.frameBufferId
                || glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING) != target.frameBufferId
                || glGetInteger(GL_READ_BUFFER) == GL_NONE || glGetInteger(GL_SAMPLE_BUFFERS) != 0
                || glGetNamedFramebufferAttachmentParameteri(target.frameBufferId, GL_DEPTH_ATTACHMENT,
                        GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME) != target.getDepthTextureId()) {
            return false;
        }
        return supportedViewport(target.width, target.height)
                && glCheckNamedFramebufferStatus(target.frameBufferId, GL_READ_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE;
    }

    private static boolean supportedViewport(int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var viewport = stack.mallocInt(4);
            var depthRange = stack.mallocFloat(2);
            glGetIntegerv(GL_VIEWPORT, viewport);
            glGetFloatv(GL_DEPTH_RANGE, depthRange);
            return viewport.get(0) == 0 && viewport.get(1) == 0
                    && viewport.get(2) == width && viewport.get(3) == height
                    && glGetInteger(GL_CLIP_ORIGIN) == GL_LOWER_LEFT
                    && glGetInteger(GL_CLIP_DEPTH_MODE) == GL_NEGATIVE_ONE_TO_ONE
                    && depthRange.get(0) == 0.0f && depthRange.get(1) == 1.0f;
        }
    }

    static boolean cullingEnabled() {
        return cullingEnabled(true);
    }

    static boolean cullingEnabled(boolean boar) {
        return Config.citadelPartCullingEnabled || (boar && Config.citadelBottomFaceCullingEnabled);
    }

    static boolean dispatch(int count, int vertices, int offset, PartBounds parts, boolean boar) {
        boolean probe = boar && Config.citadelBottomFaceProbeEnabled && !probeComplete;
        boolean requestCull = parts != null && cullingEnabled(boar);
        if (unavailable || (!probe && !requestCull) || count <= 0
                || vertices <= 0 || vertices > MAX_MESH_VERTICES || vertices % 4 != 0) {
            return false;
        }
        boolean supportedDraw = requestCull && CoreFeature.getDrawMethod() == BaseVertexDrawMethod.INSTANCE;
        armed |= probe;
        cullingArmed |= supportedDraw;
        int samples = collecting && probe ? Math.min(count, MAX_INSTANCES - sampledInstances) : 0;
        boolean skipParts = culling && supportedDraw && Config.citadelPartCullingEnabled;
        boolean skipFaces = culling && supportedDraw && !skipParts && boar && Config.citadelBottomFaceCullingEnabled;
        if (!captured || (samples == 0 && !skipParts && !skipFaces)
                || glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING) != framebuffer
                || glGetNamedFramebufferAttachmentParameteri(framebuffer, GL_DEPTH_ATTACHMENT,
                        GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME) != depthAttachment
                || !projection.equals(RenderSystem.getProjectionMatrix())
                || !modelView.equals(RenderSystem.getModelViewMatrix()) || !supportedViewport(width, height)) {
            return false;
        }
        int quadsPerInstance = vertices / 4;
        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int previousBuffer = glGetInteger(GL_SHADER_STORAGE_BUFFER_BINDING);
        BufferBinding previousCounters = BufferBinding.capture(COUNTER_BINDING);
        int previousActiveUnit = glGetInteger(GL_ACTIVE_TEXTURE);
        int previousSampler = glGetIntegeri(GL_SAMPLER_BINDING, DEPTH_UNIT);
        glActiveTexture(GL_TEXTURE0 + DEPTH_UNIT);
        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var matrix = stack.mallocFloat(16);
            projection.mul(modelView, new Matrix4f()).get(matrix);
            glBindTexture(GL_TEXTURE_2D, snapshot);
            glBindSampler(DEPTH_UNIT, 0);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, COUNTER_BINDING, counters);
            glUseProgram(program);
            glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
            glUniform1ui(1, vertices);
            glUniform1ui(2, offset);
            glUniformMatrix4fv(3, false, matrix);
            glUniform1ui(7, samples);
            if (samples != 0 || skipFaces) {
                int instances = skipFaces ? Math.min(count, MAX_CULL_INSTANCES) : samples;
                glUniform1ui(0, instances);
                glUniform1i(8, skipFaces ? 1 : 0);
                glUniform1ui(9, 0);
                glDispatchCompute((instances * quadsPerInstance + 63) / 64, 1, 1);
            }
            if (skipParts) {
                if (samples != 0) {
                    glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
                }
                dispatchParts(Math.min(count, MAX_CULL_INSTANCES), parts);
                sampledParts += samples * parts.count();
            }
            sampledInstances += samples;
            sampledQuads += samples * quadsPerInstance;
        } finally {
            glBindTexture(GL_TEXTURE_2D, previousTexture);
            glBindSampler(DEPTH_UNIT, previousSampler);
            glActiveTexture(previousActiveUnit);
            previousCounters.restore(COUNTER_BINDING);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, previousBuffer);
            glUseProgram(previousProgram);
        }
        return skipParts || skipFaces;
    }

    private static void dispatchParts(int instances, PartBounds parts) {
        BufferBinding previousParts = BufferBinding.capture(PART_BINDING);
        try {
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, PART_BINDING, parts.buffer());
            glUniform1ui(0, instances);
            glUniform1ui(9, parts.count());
            glDispatchCompute((instances * parts.count() + 63) / 64, 1, 1);
        } finally {
            previousParts.restore(PART_BINDING);
        }
    }

    public static void finishFrame() {
        if (!captured) {
            return;
        }
        captured = false;
        culling = false;
        if (!collecting) {
            return;
        }
        collecting = false;
        if (sampledInstances == 0) {
            warnTarget("whole-model uploads did not match the captured terrain view");
            if (captureAttempts == MAX_CAPTURE_ATTEMPTS) {
                finishProbe();
            }
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var result = stack.mallocInt(COUNTER_COUNT);
            glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT);
            glGetNamedBufferSubData(counters, 0, result);
            LOGGER.info("[FRMC/CitadelBottomFaceProbe] instances={} quads={} modelUndersides={} "
                            + "projectedCCW={} projectedCW={} depthChecked={} terrainCovered={} terrainCoveredCCW={} skippedRaster={}",
                    sampledInstances, sampledQuads, result.get(0), result.get(1), result.get(2),
                    result.get(3), result.get(4), result.get(5), result.get(6));
            if (sampledParts != 0) {
                LOGGER.info("[FRMC/CitadelPartCulling] parts={} outsideFrustum={} terrainCovered={} skippedQuads={}",
                        sampledParts, result.get(7), result.get(8), result.get(9));
            }
        } finally {
            finishProbe();
        }
    }

    private static void finishProbe() {
        probeComplete = true;
        if (!cullingEnabled()) {
            release();
        }
    }

    private static void warnTarget(String reason) {
        if (!warnedTarget && (Config.citadelBottomFaceProbeEnabled || Config.entityRenderDiagnosticsEnabled)) {
            warnedTarget = true;
            LOGGER.info("[FRMC/CitadelGeometryVisibility] Waiting for a supported view ({})", reason);
        }
    }

    static void release() {
        armed = false;
        cullingArmed = false;
        beginFrame();
        cutoutShader = null;
        cutoutNoCullShader = null;
        captureAttempts = 0;
        if (snapshot != 0) {
            glDeleteTextures(snapshot);
            snapshot = 0;
        }
        if (counters != 0) {
            glDeleteBuffers(counters);
            counters = 0;
        }
        if (program != 0) {
            glDeleteProgram(program);
            program = 0;
        }
    }
}
