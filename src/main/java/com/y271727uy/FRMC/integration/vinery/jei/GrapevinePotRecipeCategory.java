package com.y271727uy.FRMC.integration.vinery.jei;

import com.y271727uy.FRMC.FRMCMod;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class GrapevinePotRecipeCategory implements IRecipeCategory<GrapevinePotRecipeCategory.Recipe> {
    static final RecipeType<Recipe> TYPE = RecipeType.create(FRMCMod.MODID, "grapevine_pot", Recipe.class);

    private final ItemStack pot;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable plus;

    GrapevinePotRecipeCategory(IGuiHelper gui, ItemStack pot) {
        this.pot = pot;
        icon = gui.createDrawableItemStack(pot);
        arrow = gui.getRecipeArrow();
        plus = gui.getRecipePlusSign();
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return pot.getHoverName();
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 132;
    }

    @Override
    public int getHeight() {
        return 37;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        var fruit = builder.addSlot(RecipeIngredientRole.INPUT, 4, 4).setStandardSlotBackground().addItemStacks(recipe.fruit());
        if (recipe.showContainerHint()) {
            fruit.addTooltipCallback((slot, tooltip) -> tooltip.add(
                Component.translatable("frmc.jei.grapevine_pot.offhand_container",
                    recipe.container().getHoverName().copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        }
        if (!recipe.additive().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 34, 4).setStandardSlotBackground().addItemStacks(recipe.additive());
        }
        if (!recipe.container().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, recipe.additive().isEmpty() ? 34 : 64, 4)
                .setStandardSlotBackground().addItemStack(recipe.container());
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 112, 4).setOutputSlotBackground().addItemStack(recipe.output());
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        if (!recipe.additive().isEmpty() || !recipe.container().isEmpty()) {
            plus.draw(graphics, 22, 7);
        }
        if (!recipe.additive().isEmpty() && !recipe.container().isEmpty()) {
            plus.draw(graphics, 52, 7);
        }
        arrow.draw(graphics, 85, 4);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("frmc.jei.grapevine_pot.stomp"),
            4, 27, 0x404040, false);
    }

    record Recipe(List<ItemStack> fruit, List<ItemStack> additive, ItemStack container, ItemStack output,
                  boolean showContainerHint) {
    }
}
