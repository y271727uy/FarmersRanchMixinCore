package com.y271727uy.FRMC.mixin.nethers_exoticism;

import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismSupport;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.RamboutanLeavesUpdateTickProcedure", remap = false)
public abstract class RamboutanLeavesUpdateTickProcedureMixin {
    /**
     * @author FRMC
     * @reason Keep the one-percent rambutan growth, but cache the fruit state and wool sound.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z) {
        BlockPos origin = BlockPos.containing(x, y, z);
        BlockPos below = origin.below();
        if (!world.isEmptyBlock(below) || world.getRandom().nextFloat() >= 0.01F) {
            return;
        }
        NethersExoticismSupport.place(world, below, NethersExoticismSupport.rambutanFruit());
        NethersExoticismSupport.play(world, origin, NethersExoticismSupport.WOOL_PLACE, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }
}
