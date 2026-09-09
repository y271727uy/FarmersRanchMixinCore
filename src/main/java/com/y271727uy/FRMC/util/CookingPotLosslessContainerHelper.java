package com.y271727uy.FRMC.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CookingPotLosslessContainerHelper {
    public static final TagKey<Item> LOSSLESS_CONTAINERS =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("farmersdelight", "lossless"));

    private CookingPotLosslessContainerHelper() {
    }

    public static boolean isLosslessContainer(ItemStack stack) {
        return !stack.isEmpty() && stack.is(LOSSLESS_CONTAINERS);
    }
}
