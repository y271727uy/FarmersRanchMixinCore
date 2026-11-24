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
    public static final int WIDTH = 156;

    private static final int INPUT_X = 47;
    private static final int INPUT_Y = 14;
    private static final int COLUMN_SPACING = 23;
    private static final int ROW_SPACING = 18;
    private static final int OUTPUT_X = 130;
    private static final int OUTPUT_Y = 36;

    private ShakerJeiLayout() {
    }

    public static void addSlots(IRecipeLayoutBuilder builder, ShakerRecipe recipe) {
        var ingredients = recipe.getIngredients();
        for (int index = 0; index < ShakerIngredients.MAX_INPUTS; index++) {
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, inputX(index), inputY(index))
                    .setStandardSlotBackground().setSlotName(inputName(index));
            if (index < ingredients.size() && !ingredients.get(index).isEmpty()) {
                slot.addIngredients(ingredients.get(index));
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .setOutputSlotBackground().addItemStack(recipe.result());
    }

    public static void drawColors(GuiGraphics graphics, IRecipeSlotsView slots, ShakerRecipe recipe) {
        var colors = recipe.ingredientColors();
        for (int index = 0; index < ShakerIngredients.MAX_INPUTS; index++) {
            ChatFormatting definedColor = colors.get(index);
            ChatFormatting formatting = slots.findSlotByName(inputName(index))
                    .flatMap(IRecipeSlotView::getDisplayedItemStack)
                    .filter(stack -> !stack.isEmpty())
                    .map(stack -> definedColor != null && definedColor.getColor() != null
                            ? definedColor : ShakerIngredients.displayColor(stack))
                    .orElse(ChatFormatting.GRAY);
            int x = inputX(index) + 14;
            int y = inputY(index);
            graphics.fill(x, y, x + 8, y + 16, 0xFF000000 | formatting.getColor());
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
