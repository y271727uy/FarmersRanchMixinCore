package com.y271727uy.FRMC.opengl;

import com.y271727uy.FRMC.config.Config;
import com.y271727uy.FRMC.mixin.opengl.FrameTimerMixinAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FrameTimer;
import org.lwjgl.opengl.ATIMeminfo;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.NVXGPUMemoryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class OpenGlDiagnostics {
    private static final String PREFIX = "[FRMC/OpenGL]";
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenGlDiagnostics.class);

    private static boolean contextLogged;
    private static long lastFlipFrameNanos = -1L;
    private static long lastLowPercentLogMillis;
    private static long lastGpuMemoryLogMillis;
    private static long lastPhaseLogMillis;
    private static long lastThreadViolationLogMillis;

    private static long framePhaseStartNanos;
    private static String currentFramePhase;
    private static final List<String> phaseLogEntries = new ArrayList<>();

    private OpenGlDiagnostics() {
    }

    public static void onFlipFrameHead() {
        if (!Config.openglDiagnosticsEnabled) {
            return;
        }

        long now = System.nanoTime();
        if (!contextLogged) {
            contextLogged = true;
            logOpenGlContext();
        }

        if (lastFlipFrameNanos != -1L) {
            long deltaNanos = now - lastFlipFrameNanos;
            long thresholdNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0L, Config.openglStutterThresholdMs));
            if (deltaNanos >= thresholdNanos) {
                logFrameStall(deltaNanos);
            }
        }

        lastFlipFrameNanos = now;
    }

    public static void onThreadViolation(String methodName, boolean conditionSatisfied) {
        if (!Config.openglThreadViolationDetectionEnabled || conditionSatisfied) {
            return;
        }

        long now = System.currentTimeMillis();
        long intervalMs = Math.max(0L, Config.openglThreadViolationLogIntervalMs);
        if (now - lastThreadViolationLogMillis < intervalMs) {
            return;
        }
        lastThreadViolationLogMillis = now;

        Thread current = Thread.currentThread();
        LOGGER.warn(
            "{} Thread violation in {}: called from thread '{}' (id={})",
            PREFIX,
            methodName,
            current.getName(),
            current.getId()
        );
    }

    public static void beginFramePhaseBreakdown() {
        if (!Config.openglFramePhaseBreakdownEnabled) {
            return;
        }

        framePhaseStartNanos = System.nanoTime();
        currentFramePhase = "sound";
        phaseLogEntries.clear();
    }

    public static void markFramePhase(String nextPhaseName) {
        if (!Config.openglFramePhaseBreakdownEnabled || currentFramePhase == null) {
            return;
        }

        long now = System.nanoTime();
        long elapsedNanos = now - framePhaseStartNanos;
        phaseLogEntries.add(currentFramePhase + "=" + formatMillis(elapsedNanos));
        currentFramePhase = nextPhaseName;
        framePhaseStartNanos = now;
    }

    public static void finishFramePhaseBreakdown() {
        if (!Config.openglFramePhaseBreakdownEnabled || currentFramePhase == null) {
            return;
        }

        long now = System.nanoTime();
        long elapsedNanos = now - framePhaseStartNanos;
        phaseLogEntries.add(currentFramePhase + "=" + formatMillis(elapsedNanos));

        long currentMillis = System.currentTimeMillis();
        long intervalMs = Math.max(0L, Config.openglFramePhaseLogIntervalMs);
        if (currentMillis - lastPhaseLogMillis >= intervalMs) {
            lastPhaseLogMillis = currentMillis;
            LOGGER.info("{} Frame phases: {}", PREFIX, String.join(", ", phaseLogEntries));
        }

        currentFramePhase = null;
        phaseLogEntries.clear();
    }

    public static void logFrameTimerSummary() {
        if (!Config.openglLowPercentStatsEnabled) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        FrameTimer frameTimer = minecraft.getFrameTimer();
        int sampleCount = ((FrameTimerMixinAccessor) frameTimer).frmc$getLogLength();
        if (sampleCount <= 0) {
            return;
        }

        int sampleWindow = Math.min(240, sampleCount);
        long[] log = frameTimer.getLog();
        int start = frameTimer.getLogStart();
        long[] samples = new long[sampleWindow];
        for (int i = 0; i < sampleWindow; ++i) {
            samples[i] = log[frameTimer.wrapIndex(start + i)];
        }

        long now = System.currentTimeMillis();
        long intervalMs = Math.max(0L, Config.openglLowPercentLogIntervalMs);
        if (now - lastLowPercentLogMillis < intervalMs) {
            return;
        }
        lastLowPercentLogMillis = now;

        java.util.Arrays.sort(samples);
        int p1Index = Math.min(sampleWindow - 1, Math.max(0, (int) Math.floor(sampleWindow * 0.01d)));
        long p1FrameNanos = samples[p1Index];
        long averageFrameNanos = 0L;
        for (long sample : samples) {
            averageFrameNanos += sample;
        }
        averageFrameNanos /= sampleWindow;

        LOGGER.info(
            "{} 1% low: {} ms, average: {} ms, samples={}",
            PREFIX,
            formatMillis(p1FrameNanos),
            formatMillis(averageFrameNanos),
            sampleWindow
        );
    }

    public static void logGpuMemorySummary() {
        if (!Config.openglGpuMemoryMonitoringEnabled) {
            return;
        }

        long now = System.currentTimeMillis();
        long intervalMs = Math.max(0L, Config.openglGpuMemoryLogIntervalMs);
        if (now - lastGpuMemoryLogMillis < intervalMs) {
            return;
        }
        lastGpuMemoryLogMillis = now;

        GLCapabilities capabilities = GL.getCapabilities();
        if (capabilities == null) {
            return;
        }

        if (capabilities.GL_NVX_gpu_memory_info) {
            int totalKb = GL11.glGetInteger(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX);
            int freeKb = GL11.glGetInteger(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX);
            int dedicatedKb = GL11.glGetInteger(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_DEDICATED_VIDMEM_NVX);
            LOGGER.info(
                "{} GPU memory(NVX): available={} MiB, total={} MiB, dedicated={} MiB",
                PREFIX,
                freeKb / 1024,
                totalKb / 1024,
                dedicatedKb / 1024
            );
            return;
        }

        if (capabilities.GL_ATI_meminfo) {
            int freeVboKb = GL11.glGetInteger(ATIMeminfo.GL_VBO_FREE_MEMORY_ATI);
            int freeTextureKb = GL11.glGetInteger(ATIMeminfo.GL_TEXTURE_FREE_MEMORY_ATI);
            int freeRenderbufferKb = GL11.glGetInteger(ATIMeminfo.GL_RENDERBUFFER_FREE_MEMORY_ATI);
            LOGGER.info(
                "{} GPU memory(ATI): vbo={} MiB, texture={} MiB, renderbuffer={} MiB",
                PREFIX,
                freeVboKb / 1024,
                freeTextureKb / 1024,
                freeRenderbufferKb / 1024
            );
        }
    }

    private static void logOpenGlContext() {
        Minecraft minecraft = Minecraft.getInstance();
        String windowInfo = "unavailable";
        if (minecraft != null && minecraft.getWindow() != null) {
            windowInfo = minecraft.getWindow().getWidth() + "x" + minecraft.getWindow().getHeight();
        }

        Runtime runtime = Runtime.getRuntime();
        LOGGER.info(
            "{} Context: vendor='{}', renderer='{}', version='{}', glsl='{}', os='{} {}', java='{}', cpuThreads={}, maxMemory={} MiB, window={}",
            PREFIX,
            safeGlString(GL11.GL_VENDOR),
            safeGlString(GL11.GL_RENDERER),
            safeGlString(GL11.GL_VERSION),
            safeGlString(GL20.GL_SHADING_LANGUAGE_VERSION),
            System.getProperty("os.name", "unknown"),
            System.getProperty("os.version", "unknown"),
            System.getProperty("java.version", "unknown"),
            runtime.availableProcessors(),
            runtime.maxMemory() >> 20,
            windowInfo
        );
    }

    private static void logFrameStall(long deltaNanos) {
        long deltaMs = TimeUnit.NANOSECONDS.toMillis(deltaNanos);
        int glError = GL11.glGetError();
        if (glError == GL11.GL_NO_ERROR) {
            LOGGER.warn("{} Frame stall detected: {} ms since last flipFrame; possible CPU spike, GPU queue stall, or driver wait.", PREFIX, deltaMs);
            return;
        }

        LOGGER.warn(
            "{} Frame stall detected: {} ms since last flipFrame; glError=0x{} (possible GPU/driver issue).",
            PREFIX,
            deltaMs,
            Integer.toHexString(glError).toUpperCase(Locale.ROOT)
        );
    }

    private static String safeGlString(int name) {
        String value = GL11.glGetString(name);
        return value != null ? value : "unknown";
    }

    private static double formatMillis(long nanos) {
        return Math.round((nanos / 1_000_000.0d) * 100.0d) / 100.0d;
    }
}
