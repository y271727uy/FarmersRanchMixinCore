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
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.PitayaStemDownUpdateTickProcedure", remap = false)
public abstract class PitayaStemDownUpdateTickProcedureMixin {
    /**
     * @author FRMC
     * @reason Grow a pitaya only from an upward stem, reading the axis once instead of allocating a direction helper.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z) {
        if (world.getRandom().nextFloat() >= 0.05F) {
            return;
        }
        BlockPos origin = BlockPos.containing(x, y, z);
        if (NethersExoticismSupport.axisOf(world.getBlockState(origin)) != Direction.Axis.Y) {
            return;
        }
        BlockPos above = origin.above();
        if (!world.isEmptyBlock(above)) {
            return;
        }
        NethersExoticismSupport.place(world, above, NethersExoticismSupport.pitayaFruit());
        NethersExoticismSupport.play(world, origin, NethersExoticismSupport.WOOL_PLACE, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }
}
