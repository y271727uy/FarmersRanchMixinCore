package com.y271727uy.FRMC.mixin.terrablender;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import terrablender.worldgen.noise.Area;
import terrablender.worldgen.noise.PixelTransformer;

import java.util.concurrent.locks.StampedLock;

/**
 * Region uniqueness is a pure function of its coordinates, but 3.0.1.10 takes a shared
 * {@link StampedLock} even when the cache already contains the answer. Chunk generation
 * samples that cache from many workers, so the lock itself serializes them.
 */
@Mixin(value = Area.class, remap = false)
public abstract class AreaCacheMixin {
    @Shadow
    @Final
    private long[] keys;

    @Shadow
    @Final
    private int[] values;

    @Shadow
    @Final
    private int mask;

    @Shadow
    @Final
    private PixelTransformer operator;

    @Shadow
    @Final
    private StampedLock lock;

    /**
     * @author FRMC
     * @reason Read a stable cache entry without acquiring the shared lock.
     */
    @Overwrite(remap = false)
    public int get(int x, int z) {
        long key = ChunkPos.asLong(x, z);
        int index = (int) HashCommon.mix(key) & this.mask;

        long stamp = this.lock.tryOptimisticRead();
        int value = this.values[index];
        if (this.keys[index] == key && this.lock.validate(stamp)) {
            return value;
        }

        stamp = this.lock.readLock();
        try {
            if (this.keys[index] == key) {
                return this.values[index];
            }
        } finally {
            this.lock.unlockRead(stamp);
        }

        stamp = this.lock.writeLock();
        try {
            if (this.keys[index] == key) {
                return this.values[index];
            }
            value = this.operator.apply(x, z);
            this.values[index] = value;
            this.keys[index] = key;
            return value;
        } finally {
            this.lock.unlockWrite(stamp);
        }
    }
}
