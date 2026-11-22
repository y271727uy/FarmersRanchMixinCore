package com.y271727uy.FRMC.mixin.vietnam_delight.cooking;

import com.y271727uy.FRMC.util.CookingPotLosslessContainerHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.registry.ModRecipeTypes;

@Pseudo
@Mixin(targets = {
    "net.huwng.vietnamdelight.block.entity.CeramicClayPotBlockEntity",
    "net.mcreator.vietnamdelight.block.entity.CeramicClayPotBlockEntity"
}, remap = false)
public abstract class CeramicClayPotBlockEntityMixin {
    @Shadow @Final private ItemStackHandler items;
    @Shadow private int cookTime;
    @Shadow private int cookTimeTotal;

    @Unique private final ItemStackHandler frmc$recipeInputs = new ItemStackHandler(CookingPotRecipe.INPUT_SLOTS);
    @Unique private final RecipeWrapper frmc$cookingInputs = new RecipeWrapper(frmc$recipeInputs);
    @Unique private ResourceLocation frmc$lastRecipe;
    @Unique private int frmc$containerSlot;

    @Inject(method = "tick", at = @At(value = "NEW", target = "(I)Lnet/minecraft/world/SimpleContainer;",
        remap = true), cancellable = true, require = 1)
    private static void frmc$useCookingPotRecipes(
        Level level, BlockPos pos, BlockState state, @Coerce BlockEntity pot, CallbackInfo ci
    ) {
        ((CeramicClayPotBlockEntityMixin) (Object) pot).frmc$cook(level, pos);
        ci.cancel();
    }

    @Unique
    private void frmc$cook(Level level, BlockPos pos) {
        int occupied = 0;
        for (int i = 0; i < CookingPotRecipe.INPUT_SLOTS; i++) {
            ItemStack stack = items.getStackInSlot(i);
            frmc$recipeInputs.setStackInSlot(i, stack);
            if (!stack.isEmpty()) {
                occupied++;
            }
        }
        frmc$containerSlot = -1;
        CookingPotRecipe recipe = occupied == 0 ? null : frmc$findRecipe(level, occupied);
        if (recipe == null) {
            cookTime = 0;
            frmc$lastRecipe = null;
            return;
        }

        if (frmc$lastRecipe != null && !recipe.getId().equals(frmc$lastRecipe)) {
            cookTime = 0;
        }
        frmc$lastRecipe = recipe.getId();
        cookTimeTotal = recipe.getCookTime();
        ItemStack result = recipe.getResultItem(level.registryAccess());
        ItemStack supplied = frmc$containerSlot < 0 ? ItemStack.EMPTY : items.getStackInSlot(frmc$containerSlot);
        boolean consumesContainer = !supplied.isEmpty() && !CookingPotLosslessContainerHelper.isLosslessContainer(supplied);
        if (result.isEmpty() || (consumesContainer && supplied.getCount() < result.getCount())) {
            return;
        }

        ItemStack output = items.getStackInSlot(6);
        int limit = Math.min(result.getMaxStackSize(), items.getSlotLimit(6));
        if ((!output.isEmpty() && !ItemStack.isSameItemSameTags(output, result))
            || output.getCount() + result.getCount() > limit || ++cookTime < cookTimeTotal) {
            return;
        }

        for (int i = 0; i < CookingPotRecipe.INPUT_SLOTS; i++) {
            if (i == frmc$containerSlot) {
                continue;
            }
            ItemStack ingredient = items.extractItem(i, 1, false);
            if (ingredient.hasCraftingRemainingItem()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                    ingredient.getCraftingRemainingItem());
            }
        }
        if (consumesContainer) {
            items.extractItem(frmc$containerSlot, result.getCount(), false);
        }
        items.setStackInSlot(6, result.copyWithCount(output.getCount() + result.getCount()));
        cookTime = 0;
    }

    @Unique
    private CookingPotRecipe frmc$findRecipe(Level level, int occupied) {
        var manager = level.getRecipeManager();
        if (frmc$lastRecipe != null
            && manager.byKey(frmc$lastRecipe).orElse(null) instanceof CookingPotRecipe last
            && frmc$matches(last, level, occupied)) {
            return last;
        }
        for (CookingPotRecipe recipe : manager.getAllRecipesFor(ModRecipeTypes.COOKING.get())) {
            if (frmc$matches(recipe, level, occupied)) {
                return recipe;
            }
        }
        return null;
    }

    @Unique
    private boolean frmc$matches(CookingPotRecipe recipe, Level level, int occupied) {
        ItemStack container = recipe.getOutputContainer();
        if (container.isEmpty()) {
            return recipe.getIngredients().size() == occupied && recipe.matches(frmc$cookingInputs, level);
        }
        if (recipe.getIngredients().size() != occupied - 1) {
            return false;
        }
        // The original menu has six input slots. Match the ingredients with the serving container excluded.
        for (int i = 0; i < CookingPotRecipe.INPUT_SLOTS; i++) {
            ItemStack stack = frmc$recipeInputs.getStackInSlot(i);
            if (ItemStack.isSameItemSameTags(stack, container)) {
                frmc$recipeInputs.setStackInSlot(i, ItemStack.EMPTY);
                boolean matches = recipe.matches(frmc$cookingInputs, level);
                frmc$recipeInputs.setStackInSlot(i, stack);
                if (matches) {
                    frmc$containerSlot = i;
                    return true;
                }
            }
        }
        return false;
    }
}
