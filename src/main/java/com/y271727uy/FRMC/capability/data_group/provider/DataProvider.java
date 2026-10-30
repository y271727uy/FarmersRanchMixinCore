package com.y271727uy.FRMC.capability.data_group.provider;

import com.y271727uy.FRMC.capability.data_group.target.BlockTarget;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Data provider that extracts fields from items or blocks.
 * Used for specialized data access (e.g., Forestry tree species).
 */
public interface DataProvider {
    /**
     * Returns the provider ID (e.g., "forestry").
     */
    String getId();

    /**
     * Extracts a field value from an item stack.
     * @return field value, or null if not available
     */
    @Nullable
    String getItemField(ItemStack stack, String fieldName);

    /**
     * Extracts a field value from a block.
     * @return field value, or null if not available
     */
    @Nullable
    String getBlockField(BlockTarget target, String fieldName);

    /**
     * Returns true if this provider can handle the given item.
     */
    boolean supportsItem(ItemStack stack);

    /**
     * Returns true if this provider can handle the given block.
     */
    boolean supportsBlock(BlockTarget target);
}
