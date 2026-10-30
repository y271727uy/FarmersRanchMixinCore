package com.y271727uy.FRMC.mixin.nethers_exoticism;

import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismSupport;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.InstantKiwanoSaplingUpdateTickProcedure", remap = false)
public abstract class InstantKiwanoSaplingUpdateTickProcedureMixin {
    /**
     * @author FRMC
     * @reason Grow the kiwano structure from cached leaves and fruit states.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z) {
        NethersExoticismSupport.growKiwanoTree(world, BlockPos.containing(x, y, z));
    }
}
