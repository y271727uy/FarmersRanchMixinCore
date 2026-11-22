package com.y271727uy.FRMC.mixin.farmersrespite.client.jei;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.FRMCMod;
import com.y271727uy.FRMC.integration.farmersrespite.KettleRecipeFluids;
import com.y271727uy.FRMC.integration.farmersrespite.KettleSlots;
import com.y271727uy.FRMC.integration.farmersrespite.client.KettleJeiFluidRenderer;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import umpaz.farmersrespite.integration.jei.KettleUnionRecipe;
import umpaz.farmersrespite.integration.jei.category.BrewingRecipeCategory;

@Mixin(value = BrewingRecipeCategory.class, remap = false)
public abstract class BrewingRecipeCategoryMixin {
    @Unique
    private static final ResourceLocation FRMC_KETTLE_TEXTURE = new ResourceLocation(FRMCMod.MODID,
            "textures/gui/farmersrespite/jei/kettle_jei.png");

    @Unique
    private static final int FRMC_CROP_SHIFT = 18;

    @Unique
    private static final String FRMC_SET_RECIPE = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;"
            + "Lumpaz/farmersrespite/integration/jei/KettleUnionRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V";

    @Unique
    private static final String FRMC_ADD_SLOT =
            "Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;addSlot(Lmezz/jei/api/recipe/RecipeIngredientRole;II)"
                    + "Lmezz/jei/api/gui/builder/IRecipeSlotBuilder;";

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target =
            "Lmezz/jei/api/helpers/IGuiHelper;createDrawable(Lnet/minecraft/resources/ResourceLocation;IIII)"
                    + "Lmezz/jei/api/gui/drawable/IDrawableStatic;"))
    private IDrawableStatic frmc$useExpandedBackground(IGuiHelper helper, ResourceLocation texture,
                                                      int sourceX, int sourceY, int width, int height,
                                                      Operation<IDrawableStatic> original) {
        if (sourceX == 29) {
            sourceX -= FRMC_CROP_SHIFT;
            width += FRMC_CROP_SHIFT;
        }
        return original.call(helper, FRMC_KETTLE_TEXTURE, sourceX, sourceY, width, height);
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target =
            "Lmezz/jei/api/helpers/IGuiHelper;drawableBuilder(Lnet/minecraft/resources/ResourceLocation;IIII)"
                    + "Lmezz/jei/api/gui/drawable/IDrawableBuilder;"), index = 0)
    private ResourceLocation frmc$useArrowTexture(ResourceLocation original) {
        return FRMC_KETTLE_TEXTURE;
    }

    @ModifyConstant(method = FRMC_SET_RECIPE, constant = @Constant(intValue = 2, ordinal = 0), require = 1)
    private int frmc$displayAllBrewIngredients(int original) {
        return KettleSlots.INPUT_COUNT;
    }

    @WrapOperation(method = FRMC_SET_RECIPE, at = @At(value = "INVOKE", target = FRMC_ADD_SLOT, ordinal = 0), require = 1)
    private IRecipeSlotBuilder frmc$positionBrewIngredients(IRecipeLayoutBuilder builder, RecipeIngredientRole role,
                                                            int x, int y, Operation<IRecipeSlotBuilder> original) {
        int index = (y - 1) / 18;
        int column = KettleSlots.inputColumn(index);
        return original.call(builder, role, x + 18 * column, 1 + 18 * KettleSlots.inputRow(index));
    }

    @WrapOperation(method = FRMC_SET_RECIPE, at = @At(value = "INVOKE", target = FRMC_ADD_SLOT, ordinal = 1), require = 1)
    private IRecipeSlotBuilder frmc$showAllFluidInputs(IRecipeLayoutBuilder builder, RecipeIngredientRole role,
                                                       int x, int y, Operation<IRecipeSlotBuilder> original,
                                                       @Local(argsOnly = true) KettleUnionRecipe recipe) {
        IRecipeSlotBuilder slot = original.call(builder, role, x, y);
        var extra = ((KettleRecipeFluids) recipe).frmc$getAdditionalInputs();
        if (!extra.isEmpty()) {
            builder.addInvisibleIngredients(role).addIngredients(ForgeTypes.FLUID_STACK, extra);
        }
        return slot;
    }

    @WrapOperation(method = FRMC_SET_RECIPE, at = @At(value = "INVOKE", target =
            "Lmezz/jei/api/gui/builder/IRecipeSlotBuilder;setFluidRenderer(JZII)Lmezz/jei/api/gui/builder/IRecipeSlotBuilder;",
            ordinal = 0), require = 1)
    private IRecipeSlotBuilder frmc$drawFluidInputsAsSegments(IRecipeSlotBuilder slot, long capacity, boolean showCapacity,
                                                             int width, int height, Operation<IRecipeSlotBuilder> original,
                                                             @Local(argsOnly = true) KettleUnionRecipe recipe) {
        var extra = ((KettleRecipeFluids) recipe).frmc$getAdditionalInputs();
        if (extra.isEmpty()) {
            return original.call(slot, capacity, showCapacity, width, height);
        }
        return slot.setCustomRenderer(ForgeTypes.FLUID_STACK,
                new KettleJeiFluidRenderer(recipe.getFluidIn(), extra, width, height));
    }

    @WrapOperation(method = FRMC_SET_RECIPE, at = {
            @At(value = "INVOKE", target = FRMC_ADD_SLOT, ordinal = 2),
            @At(value = "INVOKE", target = FRMC_ADD_SLOT, ordinal = 3),
            @At(value = "INVOKE", target = FRMC_ADD_SLOT, ordinal = 4)
    }, require = 3)
    private IRecipeSlotBuilder frmc$alignResultSlots(IRecipeLayoutBuilder builder, RecipeIngredientRole role,
                                                    int x, int y, Operation<IRecipeSlotBuilder> original) {
        // The input tank moved left with the crop origin, so its recipe-local x stays at 3.
        return original.call(builder, role, x + FRMC_CROP_SHIFT, y);
    }

    @ModifyArg(method = "setRecipe", at = @At(value = "INVOKE", target =
            "Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;moveRecipeTransferButton(II)V"), index = 0)
    private int frmc$alignTransferButton(int x) {
        return x + FRMC_CROP_SHIFT;
    }

    @ModifyArg(method = "draw", at = @At(value = "INVOKE", target =
            "Lmezz/jei/api/gui/drawable/IDrawableAnimated;draw(Lnet/minecraft/client/gui/GuiGraphics;II)V"), index = 1)
    private int frmc$alignArrow(int x) {
        return x + FRMC_CROP_SHIFT;
    }

    @ModifyArg(method = "draw", at = @At(value = "INVOKE", target =
            "Lmezz/jei/api/gui/drawable/IDrawable;draw(Lnet/minecraft/client/gui/GuiGraphics;II)V"), index = 1)
    private int frmc$alignIndicators(int x) {
        return x + (x == 13 ? FRMC_CROP_SHIFT / 2 : FRMC_CROP_SHIFT);
    }

    @ModifyArg(method = "getTooltipStrings", at = @At(value = "INVOKE", target =
            "Lvectorwing/farmersdelight/common/utility/ClientRenderUtils;isCursorInsideBounds(IIIIDD)Z"), index = 0)
    private int frmc$alignTooltipBounds(int x) {
        return x + FRMC_CROP_SHIFT;
    }
}
