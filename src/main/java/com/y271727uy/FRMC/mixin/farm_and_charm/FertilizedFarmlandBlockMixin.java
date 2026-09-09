package com.y271727uy.FRMC.mixin.farm_and_charm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IPlantable;
import net.satisfy.farm_and_charm.core.block.FertilizedFarmlandBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(value = FertilizedFarmlandBlock.class, remap = false)
public abstract class FertilizedFarmlandBlockMixin {
    public boolean canSustainPlant(BlockState state, BlockGetter level, BlockPos pos, Direction facing, IPlantable plantable) {
        return Blocks.FARMLAND.defaultBlockState().canSustainPlant(level, pos, facing, plantable);
    }
}
