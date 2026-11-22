package com.y271727uy.FRMC.integration.kaleidoscopetavern.client;

import com.github.ysbbbbbb.kaleidoscopetavern.crafting.recipe.ShakerRecipe;
import com.y271727uy.FRMC.FRMCMod;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.ShakerIngredients;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class ShakerJeiLayout {
    public static final ResourceLocation TEXTURE = new ResourceLocation(FRMCMod.MODID,
            "textures/gui/jei/tavern/shaker_six.png");

    private static final int INPUT_X = 47;
    private static final int INPUT_Y = 14;
    private static final int COLUMN_SPACING = 22;
    private static final int ROW_SPACING = 18;
    private static final int OUTPUT_X = 123;
    private static final int OUTPUT_Y = 36;

    private ShakerJeiLayout() {
    }

    public static void addSlots(IRecipeLayoutBuilder builder, ShakerRecipe recipe) {
        var ingredients = recipe.getIngredients();
        int count = Math.min(ShakerIngredients.MAX_INPUTS, ingredients.size());
        for (int index = 0; index < count; index++) {
            var ingredient = ingredients.get(index);
            if (!ingredient.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.INPUT, inputX(index), inputY(index))
                        .setSlotName(inputName(index)).addIngredients(ingredient);
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y).addItemStack(recipe.result());
    }

    public static void drawColors(GuiGraphics graphics, IRecipeSlotsView slots, ShakerRecipe recipe) {
        var ingredients = recipe.getIngredients();
        var colors = recipe.ingredientColors();
        int count = Math.min(ShakerIngredients.MAX_INPUTS, ingredients.size());
        for (int index = 0; index < count; index++) {
            if (ingredients.get(index).isEmpty()) {
                continue;
            }
            ChatFormatting formatting = colors.get(index);
            if (formatting == null || formatting.getColor() == null) {
                formatting = slots.findSlotByName(inputName(index))
                        .flatMap(IRecipeSlotView::getDisplayedItemStack)
                        .map(ShakerIngredients::displayColor)
                        .orElse(ChatFormatting.GRAY);
            }
            int x = inputX(index) + 18;
            int y = inputY(index) + 4;
            graphics.fill(x, y, x + 2, y + 8, 0xFF000000 | formatting.getColor());
        }
    }

    private static String inputName(int index) {
        return "ingredient_" + index;
    }

    private static int inputX(int index) {
        return INPUT_X + index % 2 * COLUMN_SPACING;
    }

    private static int inputY(int index) {
        return INPUT_Y + index / 2 * ROW_SPACING;
    }
}
