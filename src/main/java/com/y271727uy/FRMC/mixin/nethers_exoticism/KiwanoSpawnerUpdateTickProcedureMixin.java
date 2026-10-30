package com.y271727uy.FRMC.mixin.nethers_exoticism;

import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismSupport;
import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismSpawners;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.KiwanoSpawnerUpdateTickProcedure", remap = false)
public abstract class KiwanoSpawnerUpdateTickProcedureMixin {
    /**
     * @author FRMC
     * @reason Grow a kiwano only with two empty blocks above, then always replace the spawner.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z) {
        BlockPos origin = BlockPos.containing(x, y, z);
        if (NethersExoticismSpawners.hasHeadroom(world, origin, 2)) {
            NethersExoticismSupport.growKiwanoTree(world, origin.above());
        }
        world.setBlock(origin, Blocks.WARPED_NYLIUM.defaultBlockState(), 3);
    }
}
