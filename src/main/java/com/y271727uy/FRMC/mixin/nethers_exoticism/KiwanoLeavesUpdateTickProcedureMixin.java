package com.y271727uy.FRMC.mixin.nethers_exoticism;

import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismSupport;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.KiwanoLeavesUpdateTickProcedure", remap = false)
public abstract class KiwanoLeavesUpdateTickProcedureMixin {
    /**
     * @author FRMC
     * @reason Grow one kiwano fruit without six repeated empty checks, registry lookups, and facing-property scans.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z) {
        if (world.getRandom().nextFloat() >= 0.01F) {
            return;
        }
        NethersExoticismSupport.growKiwanoFruit(world, BlockPos.containing(x, y, z));
    }
}
