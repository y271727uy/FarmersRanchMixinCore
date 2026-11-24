package com.y271727uy.FRMC.mixin.kaleidoscopetavern.client;

import com.github.ysbbbbbb.kaleidoscopetavern.compat.jei.category.ShakerRecipeCategory;
import com.github.ysbbbbbb.kaleidoscopetavern.crafting.recipe.ShakerRecipe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.client.ShakerJeiLayout;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShakerRecipeCategory.class, remap = false)
public abstract class ShakerRecipeCategoryMixin {
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target =
            "Lmezz/jei/api/helpers/IGuiHelper;createDrawable(Lnet/minecraft/resources/ResourceLocation;IIII)"
                    + "Lmezz/jei/api/gui/drawable/IDrawableStatic;"))
    private IDrawableStatic frmc$useShakerTexture(IGuiHelper helper, ResourceLocation texture, int u, int v,
                                                int width, int height, Operation<IDrawableStatic> original) {
        return original.call(helper, ShakerJeiLayout.TEXTURE, u, v, ShakerJeiLayout.WIDTH, height);
    }

    @ModifyConstant(method = "getWidth", constant = @Constant(intValue = 150))
    private int frmc$fitWiderSlots(int original) {
        return ShakerJeiLayout.WIDTH;
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
