package com.y271727uy.FRMC.capability.data_group.target;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Match target representing a block in the world.
 */
public final class BlockTarget implements MatchTarget {
    private final Level level;
    private final BlockPos pos;
    private final BlockState state;
    @Nullable
    private BlockEntity blockEntity;
    private boolean blockEntityResolved;

    private BlockTarget(Level level, BlockPos pos, BlockState state) {
        this.level = level;
        this.pos = pos.immutable();
        this.state = state;
    }

    public static BlockTarget of(Level level, BlockPos pos, BlockState state) {
        return new BlockTarget(level, pos, state);
    }

    public static BlockTarget of(Level level, BlockPos pos) {
        return new BlockTarget(level, pos, level.getBlockState(pos));
    }

    @Override
    public boolean isItem() {
        return false;
    }

    @Override
    public boolean isBlock() {
        return true;
    }

    @Override
    public ItemStack asItemStack() {
        throw new UnsupportedOperationException("BlockTarget cannot be converted to ItemStack");
    }

    @Override
    public BlockTarget asBlockTarget() {
        return this;
    }

    public Level getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return state;
    }

    @Nullable
    public BlockEntity getBlockEntity() {
        if (!blockEntityResolved) {
            blockEntity = level.getBlockEntity(pos);
            blockEntityResolved = true;
        }
        return blockEntity;
    }
}
