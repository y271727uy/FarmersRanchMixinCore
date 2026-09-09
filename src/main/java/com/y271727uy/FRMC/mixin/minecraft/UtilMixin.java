package com.y271727uy.FRMC.mixin.minecraft;

import net.minecraft.Util;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(Util.class)
public abstract class UtilMixin {
    @Shadow @Final @Mutable private static ExecutorService BACKGROUND_EXECUTOR;
    @Shadow @Final @Mutable private static ExecutorService IO_POOL;
    @Shadow @Final @Mutable private static AtomicInteger WORKER_COUNT;

    @Inject(method = "backgroundExecutor", at = @At("HEAD"))
    private static void frmc$replaceMain(CallbackInfoReturnable<Executor> cir) {
        if (BACKGROUND_EXECUTOR == null) BACKGROUND_EXECUTOR = newWorker("Main");
    }

    @Inject(method = "ioPool", at = @At("HEAD"))
    private static void frmc$replaceIo(CallbackInfoReturnable<Executor> cir) {
        if (IO_POOL == null) IO_POOL = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "IO-Worker-" + WORKER_COUNT.getAndIncrement());
            thread.setPriority(readInt("ioPriority", 1, 1, 10));
            return thread;
        });
    }

    private static ExecutorService newWorker(String name) {
        int parallelism = name.equals("Bootstrap") ? readInt("bootstrapThreads", 1, 1, 256) : readInt("mainThreads", 15, 1, 256);
        return new ForkJoinPool(parallelism, pool -> {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            thread.setName("Worker-" + name + "-" + WORKER_COUNT.getAndIncrement());
            thread.setPriority(name.equals("Bootstrap") ? readInt("bootstrapPriority", 1, 1, 10) : readInt("mainPriority", 1, 1, 10));
            return thread;
        }, (thread, exception) -> LogManager.getLogger("FRMC-SmoothBoot").warn("Worker failed", exception), true);
    }

    private static int readInt(String key, int fallback, int min, int max) {
        try {
            String value = System.getProperty("frmc.smoothboot." + key);
            if (value == null) return fallback;
            return Math.max(min, Math.min(max, Integer.parseInt(value)));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
