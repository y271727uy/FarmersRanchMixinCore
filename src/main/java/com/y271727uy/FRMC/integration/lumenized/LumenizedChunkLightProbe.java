package com.y271727uy.FRMC.integration.lumenized;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Client-only per-chunk Lumenized point-light counts for the overlay composite.
 * Missing Lumenized or a failed peek is 0. Call {@link #refresh()} once per overlay
 * frame, then {@link #count(long)} for each visible chunk.
 */
public final class LumenizedChunkLightProbe {
    private static final boolean LUMENIZED_PRESENT = detect();
    private static final Long2IntOpenHashMap COUNTS = new Long2IntOpenHashMap();

    private LumenizedChunkLightProbe() {
    }

    public static void refresh() {
        COUNTS.clear();
        if (!LUMENIZED_PRESENT) {
            return;
        }
        try {
            LumenizedAccess.fill(COUNTS);
        } catch (Throwable ignored) {
            COUNTS.clear();
        }
    }

    public static int count(long packedChunk) {
        return COUNTS.get(packedChunk);
    }

    private static boolean detect() {
        try {
            return ModList.get().isLoaded("lumenized");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class LumenizedAccess {
        static void fill(Long2IntOpenHashMap counts) throws ReflectiveOperationException {
            var manager = com.y271727uy.lumenized.client.light.LightManager.INSTANCE;
            if (manager == null) {
                return;
            }
            countList(counts, (List<?>) Fields.UV_LIGHT.get(manager));
            countList(counts, (List<?>) Fields.NO_UV_LIGHT.get(manager));
            Object playerMap = Fields.NO_UV_LIGHT_PLAYER.get(manager);
            if (playerMap instanceof Map<?, ?> map) {
                for (Object value : map.values()) {
                    countLight(counts, value);
                }
            }
            var itemLights = com.y271727uy.lumenized.client.light.ItemEntityLightSourceManager.getRenderLights();
            if (itemLights != null) {
                for (var light : itemLights) {
                    countLight(counts, light);
                }
            }
        }

        private static void countList(Long2IntOpenHashMap counts, List<?> lights) {
            if (lights == null) {
                return;
            }
            for (Object light : lights) {
                countLight(counts, light);
            }
        }

        private static void countLight(Long2IntOpenHashMap counts, Object value) {
            if (!(value instanceof com.y271727uy.lumenized.client.light.ColorPointLight light)) {
                return;
            }
            if (!light.enable || light.isRemoved()) {
                return;
            }
            int chunkX = SectionPos.blockToSectionCoord(Mth.floor(light.x));
            int chunkZ = SectionPos.blockToSectionCoord(Mth.floor(light.z));
            counts.addTo(ChunkPos.asLong(chunkX, chunkZ), 1);
        }
    }

    private static final class Fields {
        private static final Field UV_LIGHT;
        private static final Field NO_UV_LIGHT;
        private static final Field NO_UV_LIGHT_PLAYER;

        static {
            try {
                Class<?> manager = Class.forName("com.y271727uy.lumenized.client.light.LightManager");
                UV_LIGHT = manager.getDeclaredField("UV_LIGHT");
                UV_LIGHT.setAccessible(true);
                NO_UV_LIGHT = manager.getDeclaredField("NO_UV_LIGHT");
                NO_UV_LIGHT.setAccessible(true);
                NO_UV_LIGHT_PLAYER = manager.getDeclaredField("NO_UV_LIGHT_PLAYER");
                NO_UV_LIGHT_PLAYER.setAccessible(true);
            } catch (ReflectiveOperationException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }
    }
}
