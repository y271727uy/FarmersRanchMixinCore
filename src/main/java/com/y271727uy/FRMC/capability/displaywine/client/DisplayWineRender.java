package com.y271727uy.FRMC.capability.displaywine.client;

import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.y271727uy.FRMC.capability.displaywine.DisplayWineBottles;
import com.y271727uy.FRMC.capability.displaywine.DisplayWinePlaceholders;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.satisfy.vinery.core.registry.ObjectRegistry;

public final class DisplayWineRender {
    public static Item bottleItem(ItemStack stack, Item item) {
        if (item instanceof BlockItem || !DisplayWineBottles.isBottle(stack)) {
            return item;
        }
        return DisplayWineBottles.isLargeBottle(stack)
                ? DisplayWinePlaceholders.placeholder(stack).asItem()
                : ModBlocks.WINE.get().asItem();
    }

    public static Item vineryBottleItem(ItemStack stack, Item item) {
        if (item instanceof BlockItem || !DisplayWineBottles.isBottle(stack)) {
            return item;
        }
        return DisplayWineBottles.isLargeBottle(stack) ? ObjectRegistry.APPLE_WINE_ITEM.get()
                : ObjectRegistry.RED_WINE_ITEM.get();
    }

    public static BlockState normalize(BlockState state) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty("fake_model");
        if (property instanceof BooleanProperty booleanProperty) {
            return state.setValue(booleanProperty, false);
        }
        return state;
    }

    private DisplayWineRender() {
    }
}
