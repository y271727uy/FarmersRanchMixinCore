package com.y271727uy.FRMC.mixin.nethers_exoticism;

import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismSupport;

import net.mcreator.nethersexoticism.init.NethersExoticismModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.JaboticabaBranchEmptyUpdateTickProcedure", remap = false)
public abstract class JaboticabaBranchEmptyUpdateTickProcedureMixin {
    /**
     * @author FRMC
     * @reason Ripen an empty jaboticaba branch without copying every property through exception-driven lookups.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z) {
        if (world.getRandom().nextFloat() >= 0.01F) {
            return;
        }
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState current = world.getBlockState(pos);
        BlockState ripe = NethersExoticismSupport.copyProperties(
            current,
            NethersExoticismModBlocks.JABOTICABA_BRANCH.get().defaultBlockState()
        );
        world.setBlock(pos, ripe, 3);
        NethersExoticismSupport.play(world, pos, NethersExoticismSupport.WOOL_PLACE, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }
}
