package com.y271727uy.FRMC.integration.alexscaves;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Arrays;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;

public final class ClientBiomeSampleCache {
    private static final int WIDTH = 6;
    private static final ThreadLocal<ClientBiomeSampleCache> CACHE =
            ThreadLocal.withInitial(ClientBiomeSampleCache::new);

    @SuppressWarnings("unchecked")
    private final Holder<Biome>[] samples = (Holder<Biome>[]) new Holder<?>[WIDTH * WIDTH * WIDTH];
    private boolean active;
    private BiomeManager manager;
    private int originX;
    private int originY;
    private int originZ;

    private ClientBiomeSampleCache() {
    }

    public static void begin() {
        CACHE.get().active = true;
    }

    public static void end() {
        ClientBiomeSampleCache cache = CACHE.get();
        cache.active = false;
        cache.clearWindow();
    }

    public static Holder<Biome> sample(BiomeManager manager, int x, int y, int z,
                                       Operation<Holder<Biome>> original) {
        ClientBiomeSampleCache cache = CACHE.get();
        if (!cache.active) {
            return original.call(manager, x, y, z);
        }
        return cache.sampleWindow(manager, x, y, z, original);
    }

    private Holder<Biome> sampleWindow(BiomeManager manager, int x, int y, int z,
                                       Operation<Holder<Biome>> original) {
        int dx = x - originX;
        int dy = y - originY;
        int dz = z - originZ;
        if (this.manager != manager || dx < 0 || dx >= WIDTH || dy < 0 || dy >= WIDTH || dz < 0 || dz >= WIDTH) {
            clearWindow();
            this.manager = manager;
            originX = x;
            originY = y;
            originZ = z;
            dx = dy = dz = 0;
        }

        int index = (dx * WIDTH + dy) * WIDTH + dz;
        Holder<Biome> biome = samples[index];
        if (biome == null) {
            biome = original.call(manager, x, y, z);
            samples[index] = biome;
        }
        return biome;
    }

    private void clearWindow() {
        if (manager != null) {
            Arrays.fill(samples, null);
            manager = null;
        }
    }
}
