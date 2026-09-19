package com.y271727uy.FRMC.capability.blockchecking.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * Coarse per-chunk Starlight queue size for overlay display. Not a heat-map source.
 * Missing Starlight or a failed peek is 0.
 */
public final class StarlightChunkLightProbe {
    private static final boolean STARLIGHT_PRESENT = detect();

    private StarlightChunkLightProbe() {
    }

    public static int queuedTasks(ServerLevel level, long packedChunk) {
        if (!STARLIGHT_PRESENT || level == null) {
            return 0;
        }
        try {
            return StarlightAccess.queuedTasks(level, packedChunk);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static boolean detect() {
        try {
            return ModList.get().isLoaded("starlight");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class StarlightAccess {
        static int queuedTasks(ServerLevel level, long packedChunk) throws ReflectiveOperationException {
            var engine = level.getLightEngine();
            if (!(engine instanceof ca.spottedleaf.starlight.common.light.StarLightLightingProvider provider)) {
                return 0;
            }
            Object lightEngine = provider.getLightEngine();
            if (lightEngine == null) {
                return 0;
            }
            Object queue = LightQueueFields.LIGHT_QUEUE.get(lightEngine);
            if (queue == null) {
                return 0;
            }
            synchronized (queue) {
                Object map = LightQueueFields.CHUNK_TASKS.get(queue);
                if (!(map instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> tasksByChunk)) {
                    return 0;
                }
                Object chunkTasks = tasksByChunk.get(packedChunk);
                if (chunkTasks == null) {
                    return 0;
                }
                int changed = size(LightQueueFields.CHANGED_POSITIONS.get(chunkTasks));
                int lights = size(LightQueueFields.LIGHT_TASKS.get(chunkTasks));
                int total = changed + lights;
                return total > 0 ? total : 1;
            }
        }

        private static int size(Object value) {
            if (value instanceof Set<?> set) {
                return set.size();
            }
            if (value instanceof List<?> list) {
                return list.size();
            }
            return 0;
        }
    }

    private static final class LightQueueFields {
        private static final Field LIGHT_QUEUE;
        private static final Field CHUNK_TASKS;
        private static final Field CHANGED_POSITIONS;
        private static final Field LIGHT_TASKS;

        static {
            try {
                Class<?> iface = Class.forName("ca.spottedleaf.starlight.common.light.StarLightInterface");
                LIGHT_QUEUE = iface.getDeclaredField("lightQueue");
                LIGHT_QUEUE.setAccessible(true);
                Class<?> queue = Class.forName(
                        "ca.spottedleaf.starlight.common.light.StarLightInterface$LightQueue");
                CHUNK_TASKS = queue.getDeclaredField("chunkTasks");
                CHUNK_TASKS.setAccessible(true);
                Class<?> tasks = Class.forName(
                        "ca.spottedleaf.starlight.common.light.StarLightInterface$LightQueue$ChunkTasks");
                CHANGED_POSITIONS = tasks.getDeclaredField("changedPositions");
                CHANGED_POSITIONS.setAccessible(true);
                LIGHT_TASKS = tasks.getDeclaredField("lightTasks");
                LIGHT_TASKS.setAccessible(true);
            } catch (ReflectiveOperationException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }
    }
}
