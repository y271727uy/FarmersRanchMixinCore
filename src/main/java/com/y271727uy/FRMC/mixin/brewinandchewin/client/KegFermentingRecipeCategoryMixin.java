package com.y271727uy.FRMC.mixin.brewinandchewin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.integration.brewinandchewin.client.KegJeiTankRenderer;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import umpaz.brewinandchewin.integration.jei.KegFermentingPouringRecipe;
import umpaz.brewinandchewin.integration.jei.category.FermentingRecipeCategory;
import umpaz.brewinandchewin.common.BnCConfiguration;

@Mixin(value = FermentingRecipeCategory.class, remap = false)
public abstract class KegFermentingRecipeCategoryMixin {
    private static final String SET_RECIPE = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;"
            + "Lumpaz/brewinandchewin/integration/jei/KegFermentingPouringRecipe;"
            + "Lmezz/jei/api/recipe/IFocusGroup;)V";
    private static final String FLUID_RENDERER = "Lmezz/jei/api/gui/builder/IRecipeSlotBuilder;"
            + "setFluidRenderer(JZII)Lmezz/jei/api/gui/builder/IRecipeSlotBuilder;";

    @Shadow @Final protected IDrawable kegOverlay;

    @Inject(method = "draw(Lumpaz/brewinandchewin/integration/jei/KegFermentingPouringRecipe;"
            + "Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/client/gui/GuiGraphics;DD)V",
            at = @At("TAIL"))
    private void frmc$drawYoukaiTanks(KegFermentingPouringRecipe recipe, IRecipeSlotsView slots,
                                     GuiGraphics graphics, double mouseX, double mouseY, CallbackInfo ci) {
        int capacity = BnCConfiguration.KEG_CAPACITY.get();
        FluidStack input = recipe.getFluidIngredient();
        if (input != null && !input.isEmpty()) {
            ItemStack bottle = frmc$juiceBottle(input);
            if (!bottle.isEmpty()) {
                KegJeiTankRenderer.drawTank(graphics, input, bottle, capacity, 0, 2);
                kegOverlay.draw(graphics, 0, 2);
            }
        }
        Fluid output = recipe.getResultFluid();
        if (output != null && frmc$isYoukaiWine(output)) {
            KegJeiTankRenderer.drawTank(graphics, new FluidStack(output, recipe.getAmount()),
                    ItemStack.EMPTY, capacity, 100, 2);
            kegOverlay.draw(graphics, 100, 2);
        }
    }

    @Inject(method = "getTooltipStrings(Lumpaz/brewinandchewin/integration/jei/KegFermentingPouringRecipe;"
            + "Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;DD)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true)
    private void frmc$tooltipOverDrawnTank(KegFermentingPouringRecipe recipe, IRecipeSlotsView slots,
                                          double mouseX, double mouseY,
                                          CallbackInfoReturnable<List<Component>> cir) {
        if (KegJeiTankRenderer.isOverTank(mouseX, mouseY, 0, 2)) {
            FluidStack input = recipe.getFluidIngredient();
            if (input != null && !frmc$juiceBottle(input).isEmpty()) {
                cir.setReturnValue(KegJeiTankRenderer.tankTooltip(input));
            }
        } else if (KegJeiTankRenderer.isOverTank(mouseX, mouseY, 100, 2)) {
            Fluid output = recipe.getResultFluid();
            if (output != null && frmc$isYoukaiWine(output)) {
                cir.setReturnValue(KegJeiTankRenderer.tankTooltip(new FluidStack(output, recipe.getAmount())));
            }
        }
    }

    @WrapOperation(method = SET_RECIPE, at = @At(value = "INVOKE", ordinal = 0,
            target = "Ljava/lang/Boolean;booleanValue()Z"))
    private boolean frmc$showJuiceTank(Boolean renderFluid, Operation<Boolean> original,
                                      @Local(argsOnly = true) KegFermentingPouringRecipe recipe) {
        return !frmc$juiceBottle(recipe.getFluidIngredient()).isEmpty() || original.call(renderFluid);
    }

    @WrapOperation(method = SET_RECIPE, at = @At(value = "INVOKE", ordinal = 1,
            target = "Ljava/lang/Boolean;booleanValue()Z"))
    private boolean frmc$showWineTank(Boolean renderFluid, Operation<Boolean> original,
                                     @Local(argsOnly = true) KegFermentingPouringRecipe recipe) {
        return frmc$isYoukaiWine(recipe.getResultFluid()) || original.call(renderFluid);
    }

    @WrapOperation(method = SET_RECIPE, at = @At(value = "INVOKE", ordinal = 0, target = FLUID_RENDERER))
    private IRecipeSlotBuilder frmc$renderJuice(IRecipeSlotBuilder slot, long capacity, boolean capacityTooltip,
                                               int width, int height, Operation<IRecipeSlotBuilder> original,
                                               @Local(argsOnly = true) KegFermentingPouringRecipe recipe) {
        var bottle = frmc$juiceBottle(recipe.getFluidIngredient());
        if (bottle.isEmpty()) {
            return original.call(slot, capacity, capacityTooltip, width, height);
        }
        return KegJeiTankRenderer.attachTo(slot, recipe.getFluidIngredient());
    }

    @WrapOperation(method = SET_RECIPE, at = @At(value = "INVOKE", ordinal = 1, target = FLUID_RENDERER))
    private IRecipeSlotBuilder frmc$renderWine(IRecipeSlotBuilder slot, long capacity, boolean capacityTooltip,
                                              int width, int height, Operation<IRecipeSlotBuilder> original,
                                              @Local(argsOnly = true) KegFermentingPouringRecipe recipe) {
        if (!frmc$isYoukaiWine(recipe.getResultFluid())) {
            return original.call(slot, capacity, capacityTooltip, width, height);
        }
        return KegJeiTankRenderer.attachTo(slot, new FluidStack(recipe.getResultFluid(), recipe.getAmount()));
    }

    @Unique
    private static ItemStack frmc$juiceBottle(FluidStack fluid) {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        if (id == null || !id.getNamespace().equals("youkaishomecoming")) {
            return ItemStack.EMPTY;
        }
        return switch (id.getPath()) {
            case "red_grape_juice", "white_grape_juice", "black_grape_juice" -> {
                var item = ForgeRegistries.ITEMS.getValue(id);
                yield item == null || item == Items.AIR ? ItemStack.EMPTY
                        : new ItemStack(item, Math.max(1, fluid.getAmount() / 250));
            }
            default -> ItemStack.EMPTY;
        };
    }

    @Unique
    private static boolean frmc$isYoukaiWine(Fluid fluid) {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
        if (id == null || !id.getNamespace().equals("youkaishomecoming")) {
            return false;
        }
        return switch (id.getPath()) {
            case "red_wine", "white_wine", "burgundy", "van_allen", "champagne" -> true;
            default -> false;
        };
    }
}
