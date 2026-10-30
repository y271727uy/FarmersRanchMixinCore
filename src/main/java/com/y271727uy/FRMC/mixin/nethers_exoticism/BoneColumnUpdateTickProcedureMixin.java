package com.y271727uy.FRMC.mixin.nethers_exoticism;

import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismSupport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.BoneColumnUpdateTickProcedure", remap = false)
public abstract class BoneColumnUpdateTickProcedureMixin {
    /**
     * @author FRMC
     * @reason Grow a Buddha's hand from a horizontal bone column without reading the same axis twice.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z) {
        if (world.getRandom().nextFloat() >= 0.01F) {
            return;
        }
        BlockPos origin = BlockPos.containing(x, y, z);
        if (NethersExoticismSupport.axisOf(world.getBlockState(origin)) == Direction.Axis.Y) {
            return;
        }
        BlockPos below = origin.below();
        if (!world.isEmptyBlock(below)) {
            return;
        }
        NethersExoticismSupport.place(world, below, NethersExoticismSupport.buddhaHand());
        NethersExoticismSupport.play(world, origin, NethersExoticismSupport.WOOL_PLACE, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }
}
