package com.y271727uy.FRMC.capability.data_group.target;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Unified target for data group matching.
 * Can represent either an item or a block location.
 */
public interface MatchTarget {
    /**
     * Returns true if this target represents an item.
     */
    boolean isItem();

    /**
     * Returns true if this target represents a block.
     */
    boolean isBlock();

    /**
     * Returns the item stack if this is an item target.
     * @throws UnsupportedOperationException if this is not an item target
     */
    ItemStack asItemStack();

    /**
     * Returns the block target if this is a block target.
     * @throws UnsupportedOperationException if this is not a block target
     */
    BlockTarget asBlockTarget();

    /**
     * Creates an item target from an ItemStack.
     */
    static MatchTarget of(ItemStack stack) {
        return ItemTarget.of(stack);
    }

    /**
     * Creates a block target from block data.
     */
    static MatchTarget of(BlockTarget blockTarget) {
        return blockTarget;
    }
}
