package com.y271727uy.FRMC.integration.alexscaves;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.y271727uy.FRMC.integration.oculus.OculusIntegration;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public final class MagneticParticleBatch {
    private static MultiBufferSource.BufferSource lightningBuffers;
    private static MultiBufferSource.BufferSource orbitBuffers;
    private static boolean active;

    private MagneticParticleBatch() {
    }

    public static void begin() {
        if (RenderSystem.isOnRenderThread()) {
            active = !OculusIntegration.IS_IRIS_INSTALLED || !IrisApi.getInstance().isShaderPackInUse();
        }
    }

    public static MultiBufferSource.BufferSource getLightningBuffers() {
        if (!RenderSystem.isOnRenderThread() || !active) {
            return null;
        }
        if (lightningBuffers == null) {
            lightningBuffers = MultiBufferSource.immediate(new BufferBuilder(RenderType.lightning().bufferSize()));
        }
        return lightningBuffers;
    }

    public static MultiBufferSource.BufferSource getOrbitBuffers() {
        if (!RenderSystem.isOnRenderThread() || !active) {
            return null;
        }
        if (orbitBuffers == null) {
            // Both node colors use the same trail texture and differ only in vertex color.
            orbitBuffers = MultiBufferSource.immediate(new BufferBuilder(256));
        }
        return orbitBuffers;
    }

    public static boolean owns(MultiBufferSource.BufferSource source) {
        return source == lightningBuffers || source == orbitBuffers;
    }

    public static void end() {
        if (RenderSystem.isOnRenderThread() && active) {
            active = false;
            if (orbitBuffers != null) {
                orbitBuffers.endBatch();
            }
            if (lightningBuffers != null) {
                lightningBuffers.endBatch(RenderType.lightning());
            }
        }
    }
}
