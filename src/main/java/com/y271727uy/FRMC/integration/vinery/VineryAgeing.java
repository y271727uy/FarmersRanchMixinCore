package com.y271727uy.FRMC.integration.vinery;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class VineryAgeing {
    private static final TagKey<Item> AGEING = TagKey.create(Registries.ITEM, new ResourceLocation("vinery", "ageing"));

    private VineryAgeing() {
    }

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && stack.is(AGEING);
    }
}
