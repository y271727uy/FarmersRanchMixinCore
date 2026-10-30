package com.y271727uy.FRMC.mixin.oceanic_delight;

import com.y271727uy.FRMC.integration.oceanic_delight.OceanicDelightSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * Replaces the generated sea-grape growth procedure. MCreator emitted a 10% roll that rebuilds the
 * same position several times and then runs a command to place the next block.
 */
@Pseudo
@Mixin(targets = "net.mcreator.oceanicdelight.procedures.SeaGrapePlantOnTickUpdateProcedure", remap = false)
public abstract class SeaGrapePlantOnTickUpdateProcedureMixin {
    /**
     * @author FRMC
     * @reason Keep the original 10% growth chance and two-stem fruit rule, but place the block directly.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z) {
        if (world.getRandom().nextFloat() >= 0.1F) {
            return;
        }
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!OceanicDelightSupport.canGrowAbove(world, pos)) {
            return;
        }
        OceanicDelightSupport.growAbove(world, pos);
    }
}
