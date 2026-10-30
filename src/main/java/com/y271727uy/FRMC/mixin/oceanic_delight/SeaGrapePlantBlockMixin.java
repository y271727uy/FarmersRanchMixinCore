package com.y271727uy.FRMC.mixin.oceanic_delight;

import com.y271727uy.FRMC.integration.oceanic_delight.OceanicDelightSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MCreator generated this plant with tick rate 1. That reschedules every plant every tick, then the
 * procedure spends most of those ticks rediscovering that it cannot grow.
 */
@Pseudo
@Mixin(targets = "net.mcreator.oceanicdelight.block.SeaGrapePlantBlock", remap = false)
public abstract class SeaGrapePlantBlockMixin {
    @Unique
    private static final int FRMC$ACTIVE_TICK = 1;
    @Unique
    private static final int FRMC$IDLE_TICK = 20;

    @Inject(method = "m_213897_", at = @At("HEAD"), cancellable = true, remap = false)
    private void frmc$skipIdleGrowthTicks(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!OceanicDelightSupport.canGrowAbove(level, pos)) {
            ci.cancel();
            level.scheduleTick(pos, state.getBlock(), FRMC$IDLE_TICK);
            return;
        }
        if (random.nextFloat() >= 0.1F) {
            ci.cancel();
            level.scheduleTick(pos, state.getBlock(), FRMC$ACTIVE_TICK);
        }
    }

    @Inject(method = "m_213897_", at = @At("TAIL"), remap = false)
    private void frmc$keepGrowthScheduled(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        level.scheduleTick(pos, state.getBlock(), FRMC$ACTIVE_TICK);
    }
}
