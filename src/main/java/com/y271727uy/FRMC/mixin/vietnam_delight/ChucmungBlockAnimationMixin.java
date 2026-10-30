package com.y271727uy.FRMC.mixin.vietnam_delight;

import net.huwng.vietnamdelight.block.ChucmungBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = ChucmungBlock.class, remap = false)
public abstract class ChucmungBlockAnimationMixin {
    @Unique private static volatile long frmc$lastAnimationNanos;
    @Unique private static final long FRMC_ANIMATION_COOLDOWN_NANOS = 15_000_000_000L;

    @Inject(method = "createHeartAndTextAnimation", at = @At("HEAD"), cancellable = true, remap = false)
    private static void frmc$limitConcurrentAnimations(
            ServerLevel level, BlockPos pos, ParticleOptions particle, CallbackInfo ci) {
        long now = System.nanoTime();
        synchronized (ChucmungBlockAnimationMixin.class) {
            if (now - frmc$lastAnimationNanos < FRMC_ANIMATION_COOLDOWN_NANOS) {
                ci.cancel();
                return;
            }
            frmc$lastAnimationNanos = now;
        }
    }
}
