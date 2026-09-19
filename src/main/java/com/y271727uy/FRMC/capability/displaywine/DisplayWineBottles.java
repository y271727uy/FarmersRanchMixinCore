package com.y271727uy.FRMC.capability.displaywine;

import net.minecraft.world.item.ItemStack;

public final class DisplayWineBottles {
    public static boolean isSmallBottle(ItemStack stack) {
        return stack.is(DisplayWineTags.SMALL_BOTTLE_WINE);
    }

    public static boolean isLargeBottle(ItemStack stack) {
        return stack.is(DisplayWineTags.LARGE_BOTTLE_WINE);
    }

    public static boolean isBottle(ItemStack stack) {
        return isSmallBottle(stack) || isLargeBottle(stack);
    }

    private DisplayWineBottles() {
    }
}
