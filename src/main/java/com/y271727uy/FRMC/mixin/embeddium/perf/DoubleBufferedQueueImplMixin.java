package com.y271727uy.FRMC.mixin.embeddium.perf;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 入队时队列满了只扩 1 格，连续入队就是每次都整体拷贝；改成和 ensureCapacity 一样至少翻倍。 */
@Mixin(targets = "me.jellysquid.mods.sodium.client.util.collections.DoubleBufferedQueue$QueueImpl", remap = false)
public abstract class DoubleBufferedQueueImplMixin {
    @Shadow private Object[] elements;

    @ModifyArg(method = "enqueue", at = @At(value = "INVOKE", target = "resize(I)V"), require = 0)
    private int frmc$growGeometrically(int minimumSize) {
        return Math.max(minimumSize, this.elements.length << 1);
    }
}
