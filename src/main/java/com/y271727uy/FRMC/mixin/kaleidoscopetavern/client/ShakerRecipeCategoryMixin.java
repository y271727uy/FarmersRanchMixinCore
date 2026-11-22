package com.y271727uy.FRMC.mixin.kaleidoscopetavern.client;

import com.github.ysbbbbbb.kaleidoscopetavern.compat.jei.category.ShakerRecipeCategory;
import com.github.ysbbbbbb.kaleidoscopetavern.crafting.recipe.ShakerRecipe;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.client.ShakerJeiLayout;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShakerRecipeCategory.class, remap = false)
public abstract class ShakerRecipeCategoryMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target =
            "Lmezz/jei/api/helpers/IGuiHelper;createDrawable(Lnet/minecraft/resources/ResourceLocation;IIII)"
                    + "Lmezz/jei/api/gui/drawable/IDrawableStatic;"), index = 0)
    private ResourceLocation frmc$useShakerTexture(ResourceLocation original) {
        return ShakerJeiLayout.TEXTURE;
    }

    @Inject(method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;"
            + "Lcom/github/ysbbbbbb/kaleidoscopetavern/crafting/recipe/ShakerRecipe;"
            + "Lmezz/jei/api/recipe/IFocusGroup;)V", at = @At("HEAD"), cancellable = true)
    private void frmc$layoutShakerSlots(IRecipeLayoutBuilder builder, ShakerRecipe recipe, IFocusGroup focuses,
                                      CallbackInfo ci) {
        ShakerJeiLayout.addSlots(builder, recipe);
        ci.cancel();
    }

    @Inject(method = "draw(Lcom/github/ysbbbbbb/kaleidoscopetavern/crafting/recipe/ShakerRecipe;"
            + "Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/client/gui/GuiGraphics;DD)V",
            at = @At(value = "INVOKE", target =
                    "Lcom/github/ysbbbbbb/kaleidoscopetavern/crafting/recipe/ShakerRecipe;"
                            + "ingredientColors()Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;"), cancellable = true)
    private void frmc$drawShakerColors(ShakerRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                                      double mouseX, double mouseY, CallbackInfo ci) {
        ShakerJeiLayout.drawColors(graphics, slots, recipe);
        ci.cancel();
    }
}
