package com.y271727uy.FRMC.integration.diagnostics;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.integration.citadel.render.ar.CitadelGpuPoseProgram;
import com.y271727uy.FRMC.integration.citadel.render.ar.CitadelGpuPoseProgram.BufferBinding;
import com.y271727uy.FRMC.integration.citadel.model.CitadelSkeletonLayoutCache;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import static org.lwjgl.opengl.GL45C.*;

/** Compares one batch of GPU model-space matrices against the Citadel PoseStack operations. */
final class CitadelGpuPoseValidation {
    static final int MAX_NODES = 64;
    private static final int INPUT_STRIDE = 64;
    private static final int OUTPUT_STRIDE = 112;
    private static final int TIMED_DISPATCHES = 8;
    private static final float MAX_ERROR = 1.0E-4F;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean attempted;

    private CitadelGpuPoseValidation() {
    }

    static boolean pending() {
        return !attempted;
    }

    static void validate(List<CitadelPoseSnapshot> batch) {
        if (attempted || batch.isEmpty() || !RenderSystem.isOnRenderThread()) {
            return;
        }
        CitadelSkeletonLayoutCache.Topology topology = batch.get(0).topology();
        int nodes = topology.nodeCount();
        if (nodes == 0 || nodes > MAX_NODES || !supports(batch, topology)) {
            return;
        }
        attempted = true;
        if (!GL.getCapabilities().OpenGL45) {
            LOGGER.info("[FRMC/CitadelGpuPose] Skipped because OpenGL 4.5 is unavailable");
            return;
        }

        int poses = batch.size() * nodes;
        ByteBuffer input = MemoryUtil.memAlloc(poses * INPUT_STRIDE);
        ByteBuffer output = MemoryUtil.memAlloc(poses * OUTPUT_STRIDE);
        try {
            int depth = pack(batch, topology, input);
            Timing timing = dispatch(input, output, batch.size(), nodes, depth);
            float error = compare(batch, topology, output);
            if (error <= MAX_ERROR) {
                LOGGER.info("[FRMC/CitadelGpuPose] Validated {} poses x {} bones, maxError={}, GPU dispatch avg={} us, readback={} us",
                        batch.size(), nodes, error, timing.dispatchNanos() / 1000.0, timing.readbackNanos() / 1000.0);
            } else {
                LOGGER.warn("[FRMC/CitadelGpuPose] Matrix comparison failed for {} poses x {} bones, maxError={}",
                        batch.size(), nodes, error);
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("[FRMC/CitadelGpuPose] Validation could not run", exception);
        } finally {
            MemoryUtil.memFree(output);
            MemoryUtil.memFree(input);
        }
    }

    private static boolean supports(List<CitadelPoseSnapshot> batch, CitadelSkeletonLayoutCache.Topology topology) {
        for (CitadelPoseSnapshot snapshot : batch) {
            for (int node = 0; node < topology.nodeCount(); node++) {
                int parent = topology.parentIndices()[node];
                if (parent >= node || (parent >= 0 && topology.depths()[parent] >= topology.depths()[node])) {
                    return false;
                }
                for (int field = 0; field < 9; field++) {
                    float value = snapshot.value(node, field);
                    if (!Float.isFinite(value) || (field >= 6 && value <= 0.0F)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static int pack(List<CitadelPoseSnapshot> batch, CitadelSkeletonLayoutCache.Topology topology,
                            ByteBuffer input) {
        int maxDepth = 0;
        for (CitadelPoseSnapshot snapshot : batch) {
            for (int node = 0; node < topology.nodeCount(); node++) {
                input.putFloat(snapshot.value(node, 0) / 16.0F).putFloat(snapshot.value(node, 1) / 16.0F)
                        .putFloat(snapshot.value(node, 2) / 16.0F).putFloat(0);
                input.putFloat(snapshot.value(node, 3)).putFloat(snapshot.value(node, 4))
                        .putFloat(snapshot.value(node, 5)).putFloat(0);
                input.putFloat(snapshot.value(node, 6)).putFloat(snapshot.value(node, 7))
                        .putFloat(snapshot.value(node, 8)).putFloat(0);
                int depth = topology.depths()[node];
                input.putInt(topology.parentIndices()[node]).putInt(depth).putInt(snapshot.scalesChildren(node) ? 1 : 0)
                        .putInt(0);
                maxDepth = Math.max(maxDepth, depth);
            }
        }
        input.flip();
        return maxDepth;
    }

    private static Timing dispatch(ByteBuffer input, ByteBuffer output, int entities, int nodes, int depth)
            throws IOException {
        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int previousBuffer = glGetInteger(GL_SHADER_STORAGE_BUFFER_BINDING);
        BufferBinding previousInput = BufferBinding.capture(0);
        BufferBinding previousOutput = BufferBinding.capture(1);
        int program = CitadelGpuPoseProgram.compileProgram();
        int inputBuffer = glCreateBuffers();
        int outputBuffer = glCreateBuffers();
        int startQuery = glGenQueries();
        int endQuery = glGenQueries();
        try {
            glNamedBufferStorage(inputBuffer, input, 0);
            glNamedBufferStorage(outputBuffer, output.remaining(), 0);
            glUseProgram(program);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, inputBuffer);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, outputBuffer);
            glUniform1i(0, nodes);
            glUniform1i(1, depth);
            glDispatchCompute(entities, 1, 1);
            glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
            glQueryCounter(startQuery, GL_TIMESTAMP);
            for (int i = 0; i < TIMED_DISPATCHES; i++) {
                glDispatchCompute(entities, 1, 1);
                glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
            }
            glQueryCounter(endQuery, GL_TIMESTAMP);
            long dispatchNanos = (glGetQueryObjecti64(endQuery, GL_QUERY_RESULT)
                    - glGetQueryObjecti64(startQuery, GL_QUERY_RESULT)) / TIMED_DISPATCHES;
            glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT);
            long started = System.nanoTime();
            glGetNamedBufferSubData(outputBuffer, 0, output);
            return new Timing(dispatchNanos, System.nanoTime() - started);
        } finally {
            glUseProgram(previousProgram);
            previousInput.restore(0);
            previousOutput.restore(1);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, previousBuffer);
            glDeleteQueries(startQuery);
            glDeleteQueries(endQuery);
            glDeleteBuffers(inputBuffer);
            glDeleteBuffers(outputBuffer);
            glDeleteProgram(program);
        }
    }

    private static float compare(List<CitadelPoseSnapshot> batch, CitadelSkeletonLayoutCache.Topology topology,
                                 ByteBuffer output) {
        int nodes = topology.nodeCount();
        Matrix4f[] inheritedPositions = new Matrix4f[nodes];
        Matrix3f[] inheritedNormals = new Matrix3f[nodes];
        for (int node = 0; node < nodes; node++) {
            inheritedPositions[node] = new Matrix4f();
            inheritedNormals[node] = new Matrix3f();
        }
        PoseStack stack = new PoseStack();
        Quaternionf rotation = new Quaternionf();
        Matrix4f position = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        float maxError = 0.0F;
        int offset = 0;
        for (CitadelPoseSnapshot snapshot : batch) {
            for (int node = 0; node < nodes; node++) {
                int parent = topology.parentIndices()[node];
                if (parent >= 0) {
                    position.set(inheritedPositions[parent]);
                    normal.set(inheritedNormals[parent]);
                } else {
                    position.identity();
                    normal.identity();
                }
                stack.translate(snapshot.value(node, 0) / 16.0F, snapshot.value(node, 1) / 16.0F,
                        snapshot.value(node, 2) / 16.0F);
                stack.mulPose(rotation.rotationZYX(snapshot.value(node, 5), snapshot.value(node, 4),
                        snapshot.value(node, 3)));
                float x = snapshot.value(node, 6);
                float y = snapshot.value(node, 7);
                float z = snapshot.value(node, 8);
                stack.scale(x, y, z);
                for (int column = 0; column < 4; column++) {
                    for (int row = 0; row < 4; row++) {
                        maxError = Math.max(maxError, Math.abs(position.get(column, row)
                                - output.getFloat(offset + column * 16 + row * 4)));
                    }
                }
                for (int column = 0; column < 3; column++) {
                    for (int row = 0; row < 3; row++) {
                        maxError = Math.max(maxError, Math.abs(normal.get(column, row)
                                - output.getFloat(offset + 64 + column * 16 + row * 4)));
                    }
                }
                if (!snapshot.scalesChildren(node)) {
                    stack.scale(1.0F / Math.max(x, 0.0001F), 1.0F / Math.max(y, 0.0001F),
                            1.0F / Math.max(z, 0.0001F));
                }
                inheritedPositions[node].set(position);
                inheritedNormals[node].set(normal);
                offset += OUTPUT_STRIDE;
            }
        }
        return maxError;
    }

    private record Timing(long dispatchNanos, long readbackNanos) {}

}
