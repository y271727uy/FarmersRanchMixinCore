package com.y271727uy.FRMC.mixin.minecraft.worldgen;

import com.y271727uy.FRMC.capability.worldgen.sampler.ClimateMemoPolicy;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code RandomState} 构造长期气候 sampler 时用这个访问器把缓存标记全部剥掉，导致共享子树（shift_x、continents 等）
 * 每次采样都被重复计算。这里把能证明与 y 无关的 {@code flat_cache}/{@code cache_2d} 换成 xz 记忆节点。
 *
 * <p>访问器自带按 equals 去重的 HashMap，同一个标记只会走到这里一次，6 个气候函数共享同一个记忆节点。
 * 区块填充用的 NoiseChunk sampler 不经过这个访问器，不受影响。
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.RandomState$1")
public abstract class RandomStateClimateSamplerMixin {
    @Inject(method = "wrapNew", at = @At("HEAD"), cancellable = true)
    private void frmc$memoizeFlatCaches(DensityFunction function, CallbackInfoReturnable<DensityFunction> cir) {
        DensityFunction memo = ClimateMemoPolicy.memoize(function);
        if (memo != null) {
            cir.setReturnValue(memo);
        }
    }
}
