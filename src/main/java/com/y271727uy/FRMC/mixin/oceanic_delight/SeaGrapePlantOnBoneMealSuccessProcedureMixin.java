package com.y271727uy.FRMC.mixin.oceanic_delight;

import com.y271727uy.FRMC.integration.oceanic_delight.OceanicDelightSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

/** Bone meal uses the same generated command path as natural growth. */
@Pseudo
@Mixin(targets = "net.mcreator.oceanicdelight.procedures.SeaGrapePlantOnBoneMealSuccessProcedure", remap = false)
public abstract class SeaGrapePlantOnBoneMealSuccessProcedureMixin {
    /**
     * @author FRMC
     * @reason Place the next sea-grape stem directly instead of executing {@code /setblock}.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (OceanicDelightSupport.canGrowAbove(world, pos)) {
            OceanicDelightSupport.growAbove(world, pos);
        }
    }
}
