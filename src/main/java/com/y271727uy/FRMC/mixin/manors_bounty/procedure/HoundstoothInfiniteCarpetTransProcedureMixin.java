package com.y271727uy.FRMC.mixin.manors_bounty.procedure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "net.mcreator.manors_bounty.procedures.HoundstoothInfiniteCarpetTransProcedure", remap = false)
public abstract class HoundstoothInfiniteCarpetTransProcedureMixin {
    @Unique
    private static final int[] FRMC$VARIANT_BY_NEIGHBORS = {
            0, 12, 10, 11, 15, 9, 3, 6, 13, 7, 1, 4, 14, 8, 2, 5
    };

    /**
     * @author FRMC
     * @reason Look up the original variant directly and skip unchanged block states.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockPos.MutableBlockPos neighbor = pos.mutable();
        Block carpet = blockstate.getBlock();
        // The lookup mask uses north, south, west and east in that order.
        int mask = world.getBlockState(neighbor.setWithOffset(pos, Direction.NORTH)).is(carpet) ? 1 : 0;
        if (world.getBlockState(neighbor.setWithOffset(pos, Direction.SOUTH)).is(carpet)) {
            mask |= 2;
        }
        if (world.getBlockState(neighbor.setWithOffset(pos, Direction.WEST)).is(carpet)) {
            mask |= 4;
        }
        if (world.getBlockState(neighbor.setWithOffset(pos, Direction.EAST)).is(carpet)) {
            mask |= 8;
        }

        BlockState current = world.getBlockState(pos);
        int variant = FRMC$VARIANT_BY_NEIGHBORS[mask];
        if (current.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty property
                && current.getValue(property) != variant && property.getPossibleValues().contains(variant)) {
            world.setBlock(pos, current.setValue(property, variant), Block.UPDATE_ALL);
        }
    }
}
