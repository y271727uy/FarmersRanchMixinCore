package com.y271727uy.FRMC.integration.vinery;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Transient attribution for the entity that last crushed a grapevine pot. */
public final class GrapevinePotCrusherCache {
    private static final Map<Key, String> LAST_CRUSHER = new HashMap<>();

    private GrapevinePotCrusherCache() {
    }

    public static void set(Level level, BlockPos pos, Entity entity) {
        synchronized (LAST_CRUSHER) {
            LAST_CRUSHER.put(new Key(level.dimension(), level.isClientSide, pos), entity.getName().getString());
        }
    }

    public static String get(Level level, BlockPos pos) {
        synchronized (LAST_CRUSHER) {
            return LAST_CRUSHER.getOrDefault(new Key(level.dimension(), level.isClientSide, pos), "");
        }
    }

    public static void clear(Level level, BlockPos pos) {
        synchronized (LAST_CRUSHER) {
            LAST_CRUSHER.remove(new Key(level.dimension(), level.isClientSide, pos));
        }
    }

    private record Key(ResourceKey<Level> dimension, boolean clientSide, BlockPos pos) {
        private Key {
            pos = pos.immutable();
        }
    }
}
