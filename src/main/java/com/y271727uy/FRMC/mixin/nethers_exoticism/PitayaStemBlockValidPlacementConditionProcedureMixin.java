package com.y271727uy.FRMC.mixin.nethers_exoticism;

import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismSupport;

import net.mcreator.nethersexoticism.init.NethersExoticismModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.PitayaStemBlockValidPlacementConditionProcedure", remap = false)
public abstract class PitayaStemBlockValidPlacementConditionProcedureMixin {
    /**
     * @author FRMC
     * @reason Preserve the generated attachment rules, including its north/south sturdy-face quirk, with one axis read.
     */
    @Overwrite(remap = false)
    public static boolean execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
        BlockPos pos = BlockPos.containing(x, y, z);
        Direction.Axis axis = NethersExoticismSupport.axisOf(blockstate);
        if (axis == Direction.Axis.Y) {
            return attached(world, pos.above(), Direction.DOWN) || attached(world, pos.below(), Direction.UP);
        }
        if (axis == Direction.Axis.Z) {
            // The generated procedure checks the north neighbor against the upward face. That quirk is preserved.
            return attached(world, pos.south(), Direction.NORTH) || attached(world, pos.north(), Direction.UP);
        }
        return attached(world, pos.west(), Direction.WEST) || attached(world, pos.east(), Direction.EAST);
    }

    private static boolean attached(LevelAccessor world, BlockPos pos, Direction sturdyFace) {
        BlockState state = world.getBlockState(pos);
        return state.isFaceSturdy(world, pos, sturdyFace) || state.is(NethersExoticismModBlocks.PITAYA_STEM.get());
    }
}
