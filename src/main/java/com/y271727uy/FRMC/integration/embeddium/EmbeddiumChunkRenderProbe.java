package com.y271727uy.FRMC.integration.embeddium;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Client-only coarse Embeddium section counts for the hovered chunk.
 * Missing Embeddium or a failed peek omits the overlay line; this is not a heat-map source.
 */
public final class EmbeddiumChunkRenderProbe {
    private static final boolean EMBEDDIUM_PRESENT = detect();

    public record Probe(int rebuildSections, int geometrySections) {
    }

    private EmbeddiumChunkRenderProbe() {
    }

    public static Probe probe(int chunkX, int chunkZ) {
        if (!EMBEDDIUM_PRESENT) {
            return null;
        }
        try {
            return EmbeddiumAccess.probe(chunkX, chunkZ);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean detect() {
        try {
            return ModList.get().isLoaded("embeddium");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class EmbeddiumAccess {
        static Probe probe(int chunkX, int chunkZ) throws ReflectiveOperationException {
            var renderer = me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer.instanceNullable();
            if (renderer == null) {
                return null;
            }
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return null;
            }
            Object manager = Fields.RENDER_SECTION_MANAGER.get(renderer);
            if (manager == null) {
                return null;
            }
            int rebuild = 0;
            int geometry = 0;
            int minY = level.getMinSection();
            int maxY = level.getMaxSection();
            for (int sectionY = minY; sectionY < maxY; sectionY++) {
                Object section = Fields.GET_RENDER_SECTION.invoke(manager, chunkX, sectionY, chunkZ);
                if (!(section instanceof me.jellysquid.mods.sodium.client.render.chunk.RenderSection renderSection)) {
                    continue;
                }
                if (renderSection.getPendingUpdate() != null) {
                    rebuild++;
                }
                if ((renderSection.getFlags()
                        & me.jellysquid.mods.sodium.client.render.chunk.RenderSectionFlags.HAS_BLOCK_GEOMETRY) != 0) {
                    geometry++;
                }
            }
            return new Probe(rebuild, geometry);
        }
    }

    private static final class Fields {
        private static final Field RENDER_SECTION_MANAGER;
        private static final Method GET_RENDER_SECTION;

        static {
            try {
                Class<?> renderer = Class.forName(
                        "me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer");
                RENDER_SECTION_MANAGER = renderer.getDeclaredField("renderSectionManager");
                RENDER_SECTION_MANAGER.setAccessible(true);
                Class<?> manager = Class.forName(
                        "me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager");
                GET_RENDER_SECTION = manager.getDeclaredMethod("getRenderSection", int.class, int.class, int.class);
                GET_RENDER_SECTION.setAccessible(true);
            } catch (ReflectiveOperationException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }
    }
}
