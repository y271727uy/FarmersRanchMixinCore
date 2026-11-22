package com.y271727uy.FRMC.integration.kaleidoscopetavern;

import com.github.ysbbbbbb.kaleidoscopetavern.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopetavern.util.ColorUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

public final class ShakerIngredients {
    public static final int MIN_INPUTS = 3;
    public static final int MAX_INPUTS = 6;

    private ShakerIngredients() {
    }

    public static boolean validCount(int count) {
        return count >= MIN_INPUTS && count <= MAX_INPUTS;
    }

    public static CompoundTag expandStorage(CompoundTag storage) {
        if (storage.getInt("Size") >= MAX_INPUTS) {
            return storage;
        }
        CompoundTag expanded = storage.copy();
        expanded.putInt("Size", MAX_INPUTS);
        return expanded;
    }

    public static boolean hasEnoughToShake(ItemStackHandler storage) {
        int filled = 0;
        for (int slot = 0; slot < storage.getSlots(); slot++) {
            if (!storage.getStackInSlot(slot).isEmpty() && ++filled == MIN_INPUTS) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInRecipe(Level level, ItemStack stack) {
        return !stack.isEmpty() && level.getRecipeManager().getAllRecipesFor(ModRecipes.SHAKER_RECIPE).stream()
                .anyMatch(recipe -> recipe.ingredients().stream().anyMatch(ingredient -> ingredient.test(stack)));
    }

    public static ChatFormatting displayColor(ItemStack stack) {
        for (var entry : ColorUtils.COCKTAIL_INGREDIENT_COLORS.entrySet()) {
            if (stack.is(entry.getKey())) {
                return entry.getValue();
            }
        }
        return ChatFormatting.GRAY;
    }
}
