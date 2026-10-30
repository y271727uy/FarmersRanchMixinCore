package com.y271727uy.FRMC.mixin.nethers_exoticism;

import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismSupport;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.PitayaSaplingUpdateTickProcedure", remap = false)
public abstract class PitayaSaplingUpdateTickProcedureMixin {
    /**
     * @author FRMC
     * @reason Keep the 55 percent tall pitaya variant while using cached stem and fruit states.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z) {
        NethersExoticismSupport.growPitaya(world, BlockPos.containing(x, y, z), Math.random() < 0.55D);
    }
}
