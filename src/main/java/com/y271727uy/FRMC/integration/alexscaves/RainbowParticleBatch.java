package com.y271727uy.FRMC.integration.alexscaves;

import com.github.alexmodguy.alexscaves.client.particle.RainbowParticle;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.MultiBufferSource;

public final class RainbowParticleBatch {
    private static Object currentParticle;
    private static MultiBufferSource.BufferSource pending;

    private RainbowParticleBatch() {
    }

    public static void beforeParticle(Object particle) {
        if (RenderSystem.isOnRenderThread()) {
            if (particle.getClass() == RainbowParticle.class) {
                currentParticle = particle;
            } else {
                flush();
            }
        }
    }

    public static boolean defer(Object particle, MultiBufferSource.BufferSource buffers) {
        if (!RenderSystem.isOnRenderThread() || particle != currentParticle) {
            return false;
        }
        pending = buffers;
        return true;
    }

    public static void beforeBuffer(Object particle, MultiBufferSource.BufferSource buffers) {
        if (RenderSystem.isOnRenderThread() && pending != null && (particle != currentParticle || pending != buffers)) {
            submitPending();
        }
    }

    public static void flush() {
        if (RenderSystem.isOnRenderThread()) {
            currentParticle = null;
            submitPending();
        }
    }

    private static void submitPending() {
        var buffers = pending;
        pending = null;
        if (buffers != null) {
            buffers.endBatch();
        }
    }
}
