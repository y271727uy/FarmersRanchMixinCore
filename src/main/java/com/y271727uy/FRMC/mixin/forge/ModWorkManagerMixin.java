package com.y271727uy.FRMC.mixin.forge;

import net.minecraftforge.fml.ModWorkManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

@Mixin(ModWorkManager.class)
@Pseudo
public abstract class ModWorkManagerMixin {
    private static final Logger FRMC_LOGGER = LogManager.getLogger("FRMC-SmoothBoot");

    /** Keeps Forge's worker setup while avoiding a cross-mod-module field access. */
    @Overwrite(remap = false)
    private static ForkJoinWorkerThread newForkJoinWorkerThread(ForkJoinPool pool) {
        ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        String name = "modloading-worker-" + thread.getPoolIndex();
        thread.setName(name);
        thread.setPriority(readInt("modLoading", 1, 1, 10));
        thread.setContextClassLoader(Thread.currentThread().getContextClassLoader());
        FRMC_LOGGER.debug("Initialized {}", name);
        return thread;
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
