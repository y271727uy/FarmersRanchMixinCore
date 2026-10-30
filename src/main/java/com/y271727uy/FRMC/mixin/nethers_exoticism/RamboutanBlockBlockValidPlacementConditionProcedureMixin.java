package com.y271727uy.FRMC.mixin.nethers_exoticism;

import net.mcreator.nethersexoticism.init.NethersExoticismModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.RamboutanBlockBlockValidPlacementConditionProcedure", remap = false)
public abstract class RamboutanBlockBlockValidPlacementConditionProcedureMixin {
    /**
     * @author FRMC
     * @reason Require rambutan leaves directly above without rebuilding the position.
     */
    @Overwrite(remap = false)
    public static boolean execute(LevelAccessor world, double x, double y, double z) {
        return world.getBlockState(BlockPos.containing(x, y + 1.0D, z)).is(NethersExoticismModBlocks.RAMBOUTAN_LEAVES.get());
    }
}
