package com.y271727uy.FRMC.integration.vinery;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

public enum TavernPressing implements StringRepresentable {
    NONE("none", null, null),
    GREEN_GRAPE("green_grape", "kaleidoscope_tavern:green_grape", "green_grape_bucket"),
    GOLD_GRAPE("gold_grape", "kaleidoscope_tavern:gold_grape", "gold_grape_bucket"),
    ICE_GRAPE("ice_grape", "kaleidoscope_tavern:ice_grape", "ice_grape_bucket"),
    SWEET_BERRIES("sweet_berries", "minecraft:sweet_berries", "sweet_berries_bucket"),
    GLOW_BERRIES("glow_berries", "minecraft:glow_berries", "glow_berries_bucket"),
    GRAPE("grape", null, "grape_bucket"),
    DATA_PACK("data_pack", null, null);

    public static final int BATCH_SIZE = 8;
    private static final TagKey<Item> GRAPES = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "fruits/grapes"));
    private static final TavernPressing[] RECIPES = values();

    private final String name;
    private final ResourceLocation fruit;
    private final ResourceLocation bucket;

    TavernPressing(String name, String fruit, String bucket) {
        this.name = name;
        this.fruit = fruit == null ? null : new ResourceLocation(fruit);
        this.bucket = bucket == null ? null : new ResourceLocation("kaleidoscope_tavern", bucket);
    }

    public static TavernPressing forInput(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        for (TavernPressing recipe : RECIPES) {
            if (recipe.fruit != null && recipe.fruit.equals(id)) {
                return recipe;
            }
        }
        return stack.is(GRAPES) ? GRAPE : NONE;
    }

    public Ingredient ingredient() {
        if (this == GRAPE) {
            return Ingredient.of(GRAPES);
        }
        Item item = fruit == null ? null : ForgeRegistries.ITEMS.getValue(fruit);
        return item == null ? Ingredient.EMPTY : Ingredient.of(item);
    }

    public ItemStack output() {
        Item item = bucket == null ? null : ForgeRegistries.ITEMS.getValue(bucket);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
