package com.y271727uy.FRMC.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public final class FruitsDelightRemainderHelper {
    private FruitsDelightRemainderHelper() {
    }

    public static ItemStack replaceGlassBottleRemainder(ItemStack sourceStack, ItemStack originalRemainder) {
        return replaceGlassBottleRemainder(sourceStack.getItem(), originalRemainder);
    }

    public static ItemStack replaceGlassBottleRemainder(Item sourceItem, ItemStack originalRemainder) {
        if (originalRemainder.isEmpty() || !originalRemainder.is(Items.GLASS_BOTTLE) || !isFruitsDelightJelly(sourceItem)) {
            return originalRemainder;
        }

        Item masonJar = resolveMasonJar();
        if (masonJar == null) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(masonJar, Math.max(originalRemainder.getCount(), 1));
    }

    public static boolean hasReplacementRemainder(Item sourceItem, boolean originalHasRemainder) {
        if (!originalHasRemainder || !isFruitsDelightJelly(sourceItem)) {
            return originalHasRemainder;
        }

        return resolveMasonJar() != null;
    }

    private static boolean isFruitsDelightJelly(Item item) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        return itemId != null && "fruitsdelight".equals(itemId.getNamespace()) && itemId.getPath().endsWith("_jelly");
    }

    private static Item resolveMasonJar() {
        ResourceLocation masonJarId = ResourceLocation.tryParse("vintagedelight:mason_jar");
        if (masonJarId == null) {
            return null;
        }

        Item masonJar = ForgeRegistries.ITEMS.getValue(masonJarId);
        if (masonJar == null || masonJar == Items.AIR) {
            return null;
        }

        return masonJar;
    }
}

