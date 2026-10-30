package com.y271727uy.FRMC.mixin.nethers_exoticism;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.JaboticabaJuiceUpdateTickProcedure", remap = false)
public abstract class JaboticabaJuiceUpdateTickProcedureMixin {
    /**
     * @author FRMC
     * @reason Spawn the juice particle only on the server and only for a live entity.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, Entity entity) {
        if (!(world instanceof ServerLevel level) || entity == null || !entity.isAlive()) {
            return;
        }
        level.sendParticles(ParticleTypes.SPLASH, entity.getX(), entity.getY(), entity.getZ(), 1, 0.1D, 0.1D, 0.1D, 0.0D);
    }
}
