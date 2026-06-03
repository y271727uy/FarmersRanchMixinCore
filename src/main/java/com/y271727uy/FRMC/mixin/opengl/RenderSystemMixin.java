package com.y271727uy.FRMC.mixin.opengl;

import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.y271727uy.FRMC.Config;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Low-noise OpenGL diagnostics for client frame timing.
 * This mixin only logs useful context and stall information; it never changes render behavior.
 */
@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();

    @Unique
    private static boolean frmc$contextLogged;

    @Unique
    private static long frmc$lastFlipFrameNanos = -1L;

    @Inject(method = "flipFrame", at = @At("HEAD"))
    private static void frmc$logFrameTiming(long limitTime, CallbackInfo ci) {
        if (!Config.openglDiagnosticsEnabled) {
            return;
        }

        long now = System.nanoTime();
        if (!frmc$contextLogged) {
            frmc$contextLogged = true;
            frmc$logOpenGlContext();
        }

        if (frmc$lastFlipFrameNanos != -1L) {
            long deltaNanos = now - frmc$lastFlipFrameNanos;
            long thresholdNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0L, Config.openglStutterThresholdMs));
            if (deltaNanos >= thresholdNanos) {
                frmc$logFrameStall(deltaNanos);
            }
        }

        frmc$lastFlipFrameNanos = now;
    }

    @Unique
    private static void frmc$logOpenGlContext() {
        Minecraft minecraft = Minecraft.getInstance();
        String windowInfo = "unavailable";
        if (minecraft.getWindow() != null) {
            windowInfo = minecraft.getWindow().getWidth() + "x" + minecraft.getWindow().getHeight();
        }

        Runtime runtime = Runtime.getRuntime();
        LOGGER.info(
            "[FRMC/OpenGL] Context: vendor='{}', renderer='{}', version='{}', glsl='{}', os='{} {}', java='{}', cpuThreads={}, maxMemory={} MiB, window={}",
            frmc$glString(GL11.GL_VENDOR),
            frmc$glString(GL11.GL_RENDERER),
            frmc$glString(GL11.GL_VERSION),
            frmc$glString(GL20.GL_SHADING_LANGUAGE_VERSION),
            System.getProperty("os.name", "unknown"),
            System.getProperty("os.version", "unknown"),
            System.getProperty("java.version", "unknown"),
            runtime.availableProcessors(),
            runtime.maxMemory() >> 20,
            windowInfo
        );
    }

    @Unique
    private static void frmc$logFrameStall(long deltaNanos) {
        long deltaMs = TimeUnit.NANOSECONDS.toMillis(deltaNanos);
        int glError = GL11.glGetError();
        if (glError == GL11.GL_NO_ERROR) {
            LOGGER.warn(
                "[FRMC/OpenGL] Frame stall detected: {} ms since last flipFrame; possible CPU spike, GPU queue stall, or driver wait.",
                deltaMs
            );
            return;
        }

        LOGGER.warn(
            "[FRMC/OpenGL] Frame stall detected: {} ms since last flipFrame; glError=0x{} (possible GPU/driver issue).",
            deltaMs,
            Integer.toHexString(glError).toUpperCase(Locale.ROOT)
        );
    }

    @Unique
    private static String frmc$glString(int name) {
        String value = GL11.glGetString(name);
        return value != null ? value : "unknown";
    }
}



