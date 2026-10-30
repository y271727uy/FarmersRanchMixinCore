package com.y271727uy.FRMC.mixin.nethers_exoticism;

import net.mcreator.nethersexoticism.init.NethersExoticismModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.BouddhaSHandBlockUpdateTickProcedure", remap = false)
public abstract class BouddhaSHandBlockUpdateTickProcedureMixin {
    /**
     * @author FRMC
     * @reason Drop an unsupported Buddha's hand with one block lookup instead of rebuilding the position repeatedly.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (world.getBlockState(pos.above()).is(NethersExoticismModBlocks.BONE_COLUMN.get())) {
            return;
        }
        Block.dropResources(world.getBlockState(pos), world, pos, null);
        world.destroyBlock(pos, false);
    }
}
