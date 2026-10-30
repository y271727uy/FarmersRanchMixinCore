package com.y271727uy.FRMC.mixin.nethers_exoticism;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.block.JaboticabaJuiceBlock", remap = false)
public abstract class JaboticabaJuiceBlockMixin {
    /**
     * The generated collision hook runs for every entity inside every juice block. One splash particle per occupied block is enough.
     */
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true, remap = true)
    private void frmc$oneParticlePerBlock(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null || !entity.isAlive() || level.isClientSide() || (level.getGameTime() & 3L) != 0L) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 1, 0.1D, 0.1D, 0.1D, 0.0D);
        }
    }
}
