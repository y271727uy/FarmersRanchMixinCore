package com.y271727uy.FRMC.capability.data_group.matcher;

import com.y271727uy.FRMC.capability.data_group.target.MatchTarget;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

/**
 * Matches items by tag.
 */
public final class ItemTagMatcher implements GroupMatcher {
    private final TagKey<Item> tag;

    public ItemTagMatcher(TagKey<Item> tag) {
        this.tag = tag;
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
        return stack.is(tag);
    }

    @Override
    public String getType() {
        return "item_tag";
    }

    public TagKey<Item> getTag() {
        return tag;
    }
}
