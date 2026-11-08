package com.y271727uy.FRMC.mixin.manors_bounty.procedure;

import com.y271727uy.FRMC.integration.manors_bounty.VineGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.manors_bounty.procedures.VineStakeRandomTickProcedure", remap = false)
public abstract class VineStakeRandomTickProcedureMixin {
    /**
     * @author FRMC
     * @reason Reuse block state reads between writes during stake growth.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z, BlockState state) {
        VineGrowth.growStake(world, BlockPos.containing(x, y, z), state);
    }
}
