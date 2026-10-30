package com.y271727uy.FRMC.capability.data_group.target;

import net.minecraft.world.item.ItemStack;

/**
 * Match target representing an item stack.
 */
public final class ItemTarget implements MatchTarget {
    private final ItemStack stack;

    private ItemTarget(ItemStack stack) {
        this.stack = stack;
    }

    public static ItemTarget of(ItemStack stack) {
        return new ItemTarget(stack);
    }

    @Override
    public boolean isItem() {
        return true;
    }

    @Override
    public boolean isBlock() {
        return false;
    }

    @Override
    public ItemStack asItemStack() {
        return stack;
    }

    @Override
    public BlockTarget asBlockTarget() {
        throw new UnsupportedOperationException("ItemTarget cannot be converted to BlockTarget");
    }

    public ItemStack getStack() {
        return stack;
    }
}
