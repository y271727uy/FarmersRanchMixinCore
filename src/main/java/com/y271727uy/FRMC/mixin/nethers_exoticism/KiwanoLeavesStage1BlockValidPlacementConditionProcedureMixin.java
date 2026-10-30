package com.y271727uy.FRMC.mixin.nethers_exoticism;

import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismSupport;

import net.mcreator.nethersexoticism.init.NethersExoticismModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.KiwanoLeavesStage1BlockValidPlacementConditionProcedure", remap = false)
public abstract class KiwanoLeavesStage1BlockValidPlacementConditionProcedureMixin {
    /**
     * @author FRMC
     * @reason Check the single opposite neighbor instead of rereading facing for all six directions.
     */
    @Overwrite(remap = false)
    public static boolean execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
        BlockPos support = BlockPos.containing(x, y, z).relative(NethersExoticismSupport.facingOf(blockstate).getOpposite());
        return world.getBlockState(support).is(NethersExoticismModBlocks.KIWANO_LEAVES.get());
    }
}
