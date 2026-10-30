package com.y271727uy.FRMC.capability.data_group.matcher;

import com.y271727uy.FRMC.capability.data_group.target.MatchTarget;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Matches items by NBT field path and optional value.
 * Supports nested paths like "ForgeCaps.Parent.genome.forestry:tree_species.active".
 */
public final class ItemNBTMatcher implements GroupMatcher {
    private final ResourceLocation itemId;
    private final String nbtPath;
    @Nullable
    private final String expectedValue;

    public ItemNBTMatcher(ResourceLocation itemId, String nbtPath) {
        this(itemId, nbtPath, null);
    }

    public ItemNBTMatcher(ResourceLocation itemId, String nbtPath, @Nullable String expectedValue) {
        this.itemId = itemId;
        this.nbtPath = nbtPath;
        this.expectedValue = expectedValue;
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

        // Check item ID first
        ResourceLocation stackId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (!itemId.equals(stackId)) {
            return false;
        }

        // Check NBT path
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return false;
        }

        Tag value = getNestedTag(tag, nbtPath);
        if (value == null) {
            return false;
        }

        // If no expected value specified, just check existence
        if (expectedValue == null) {
            return true;
        }

        // Check value match
        String actualValue = value.getAsString();
        return expectedValue.equals(actualValue);
    }

    @Nullable
    private Tag getNestedTag(CompoundTag root, String path) {
        String[] parts = path.split("\\.");
        Tag current = root;

        for (String part : parts) {
            if (!(current instanceof CompoundTag compound)) {
                return null;
            }
            if (!compound.contains(part)) {
                return null;
            }
            current = compound.get(part);
        }

        return current;
    }

    @Override
    public String getType() {
        return "item_nbt";
    }

    public ResourceLocation getItemId() {
        return itemId;
    }

    public String getNbtPath() {
        return nbtPath;
    }

    @Nullable
    public String getExpectedValue() {
        return expectedValue;
    }
}
