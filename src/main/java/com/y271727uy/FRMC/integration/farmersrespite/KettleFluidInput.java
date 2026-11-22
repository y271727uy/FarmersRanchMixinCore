package com.y271727uy.FRMC.integration.farmersrespite;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import umpaz.farmersrespite.common.block.entity.KettleBlockEntity;
import umpaz.farmersrespite.common.registry.FRRecipeTypes;

public record KettleFluidInput(FluidStack fluid, int itemCount, ItemStack container) {
    public static KettleFluidInput from(KettleBlockEntity kettle, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(Items.POTION) && PotionUtils.getPotion(stack) == Potions.WATER) {
            return new KettleFluidInput(new FluidStack(Fluids.WATER, 250), 1, new ItemStack(Items.GLASS_BOTTLE));
        }
        var handler = FluidUtil.getFluidHandler(stack.copyWithCount(1)).orElse(null);
        if (handler != null) {
            FluidStack drained = handler.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                return new KettleFluidInput(drained, 1, handler.getContainer());
            }
        }
        if (kettle.getLevel() != null) {
            for (var recipe : kettle.getLevel().getRecipeManager().getAllRecipesFor(FRRecipeTypes.KETTLE_POURING.get())) {
                ItemStack drink = recipe.getOutput();
                if (ItemStack.isSameItemSameTags(stack, drink)) {
                    return new KettleFluidInput(new FluidStack(recipe.getFluid(), recipe.getAmount()),
                            drink.getCount(), recipe.getContainer().copy());
                }
            }
        }
        return null;
    }

    public boolean fits(KettleFluidTank tank) {
        return !fluid.isEmpty() && tank.fill(fluid, FluidAction.SIMULATE) == fluid.getAmount();
    }
}
