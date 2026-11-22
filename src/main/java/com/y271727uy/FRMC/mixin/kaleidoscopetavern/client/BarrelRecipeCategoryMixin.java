package com.y271727uy.FRMC.mixin.kaleidoscopetavern.client;

import com.github.ysbbbbbb.kaleidoscopetavern.compat.jei.category.BarrelRecipeCategory;
import com.github.ysbbbbbb.kaleidoscopetavern.crafting.recipe.BarrelRecipe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.TavernBarrelWine;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BarrelRecipeCategory.class, remap = false)
public abstract class BarrelRecipeCategoryMixin {
    private static final int BARREL_CAPACITY = 4000;
    private static final int CUP_VOLUME = 250;
    private static final String SET_RECIPE = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;"
        + "Lcom/github/ysbbbbbb/kaleidoscopetavern/crafting/recipe/BarrelRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V";

    @WrapOperation(method = SET_RECIPE, at = @At(value = "INVOKE", ordinal = 0,
        target = "Lmezz/jei/api/gui/builder/IRecipeSlotBuilder;addItemStack(Lnet/minecraft/world/item/ItemStack;)Lmezz/jei/api/gui/builder/IIngredientAcceptor;"))
    private IIngredientAcceptor<?> frmc$showBucketlessFluid(IRecipeSlotBuilder slot, ItemStack bucket,
                                                           Operation<IIngredientAcceptor<?>> original,
                                                           @Local(argsOnly = true) BarrelRecipe recipe) {
        if (!bucket.isEmpty()) {
            return original.call(slot, bucket);
        }
        ItemStack container = frmc$baseContainer(recipe.fluid());
        if (!container.isEmpty()) {
            return original.call(slot, container);
        }
        return slot.setFluidRenderer(BARREL_CAPACITY, true, 16, 16).addFluidStack(recipe.fluid(), BARREL_CAPACITY);
    }

    @Unique
    private static ItemStack frmc$baseContainer(Fluid fluid) {
        ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(fluid);
        if (fluidId == null) {
            return ItemStack.EMPTY;
        }
        ResourceLocation itemId = switch (fluidId.toString()) {
            case "youkaishomecoming:black_grape_juice", "youkaishomecoming:red_grape_juice",
                 "youkaishomecoming:white_grape_juice", "brewinandchewin:beer", "brewinandchewin:strongroot_ale",
                 "brewinandchewin:mead", "brewinandchewin:bloody_mary", "brewincompatdelight:rum" -> fluidId;
            case "cosmopolitan:birch_sap" -> new ResourceLocation("cosmopolitan", "birch_sap_bottle");
            case "kubejs:keg_manors_bounty_sake" -> new ResourceLocation("manors_bounty", "sake");
            default -> null;
        };
        var item = itemId == null ? null : ForgeRegistries.ITEMS.getValue(itemId);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, BARREL_CAPACITY / CUP_VOLUME);
    }

    @WrapOperation(method = SET_RECIPE, at = @At(value = "INVOKE", remap = true,
        target = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack frmc$showConfiguredYield(ItemStack result, int count, Operation<ItemStack> original,
                                                @Local(argsOnly = true) BarrelRecipe recipe) {
        return original.call(result, TavernBarrelWine.usesConfiguredYield(recipe.id()) ? result.getCount() : count);
    }
}
