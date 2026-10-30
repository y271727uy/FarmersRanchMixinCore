package com.y271727uy.FRMC.mixin.nethers_exoticism;

import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismSupport;
import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismSpawners;

import net.mcreator.nethersexoticism.init.NethersExoticismModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.RambutanSpawnerBlockAddedProcedure", remap = false)
public abstract class RambutanSpawnerBlockAddedProcedureMixin {
    /**
     * @author FRMC
     * @reason Clear nearby rambutan spawners once, then grow only when three blocks of headroom exist.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z) {
        NethersExoticismSpawners.grow(
            world, BlockPos.containing(x, y, z),
            NethersExoticismModBlocks.RAMBUTAN_SPAWNER.get(), Blocks.CRIMSON_NYLIUM, 3, 2, 3,
            NethersExoticismSupport::growRambutanTree
        );
    }
}
