package com.y271727uy.FRMC.mixin.manors_bounty.block;

import net.mcreator.manors_bounty.block.VineArborBlock;
import net.mcreator.manors_bounty.block.VineStakeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin({VineArborBlock.class, VineStakeBlock.class})
public abstract class VineRandomTickMixin extends Block {
    protected VineRandomTickMixin(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        if (!super.isRandomlyTicking(state)) {
            return false;
        }
        // Only these states enter a growth branch or consume a random roll.
        if (state.getBlock() instanceof VineStakeBlock) {
            int stage = state.getValue(VineStakeBlock.BLOCKSTATE);
            return stage == 1 || stage == 3 || stage == 5;
        }
        return state.getValue(VineArborBlock.BLOCKSTATE) != 0;
    }
}
