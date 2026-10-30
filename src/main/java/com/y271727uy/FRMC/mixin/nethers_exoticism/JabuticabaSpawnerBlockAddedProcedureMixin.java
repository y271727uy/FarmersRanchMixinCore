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
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.JabuticabaSpawnerBlockAddedProcedure", remap = false)
public abstract class JabuticabaSpawnerBlockAddedProcedureMixin {
    /**
     * @author FRMC
     * @reason Clear nearby jaboticaba spawners once, then grow only when two blocks of headroom exist.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z) {
        NethersExoticismSpawners.grow(
            world, BlockPos.containing(x, y, z),
            NethersExoticismModBlocks.JABUTICABA_SPAWNER.get(), Blocks.SOUL_SOIL, 2, 2, 2,
            NethersExoticismSupport::growJaboticabaTree
        );
    }
}
