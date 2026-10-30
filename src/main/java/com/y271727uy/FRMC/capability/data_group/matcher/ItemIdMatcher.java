package com.y271727uy.FRMC.capability.data_group.matcher;

import com.y271727uy.FRMC.capability.data_group.target.MatchTarget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Matches items by their registry ID.
 */
public final class ItemIdMatcher implements GroupMatcher {
    private final ResourceLocation itemId;

    public ItemIdMatcher(ResourceLocation itemId) {
        this.itemId = itemId;
    }

    @Override
    public boolean matches(MatchTarget target) {
        if (!target.isItem()) {
            return false;
        }
        ItemStack stack = target.asItemStack();
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation stackId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return itemId.equals(stackId);
    }

    @Override
    public String getType() {
        return "item_id";
    }

    public ResourceLocation getItemId() {
        return itemId;
    }
}
