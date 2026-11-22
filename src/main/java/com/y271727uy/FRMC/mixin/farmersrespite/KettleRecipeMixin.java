package com.y271727uy.FRMC.mixin.farmersrespite;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.farmersrespite.KettleRecipeFluids;
import com.y271727uy.FRMC.integration.farmersrespite.KettleSlots;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import umpaz.farmersrespite.common.crafting.KettleRecipe;

@Mixin(value = KettleRecipe.class, remap = false)
public abstract class KettleRecipeMixin implements KettleRecipeFluids {
    @Unique
    private List<FluidStack> frmc$additionalInputs = List.of();

    @Override
    public List<FluidStack> frmc$getAdditionalInputs() {
        return frmc$additionalInputs;
    }

    @Override
    public void frmc$setAdditionalInputs(List<FluidStack> inputs) {
        frmc$additionalInputs = List.copyOf(inputs);
    }

    @ModifyConstant(method = "matches(Lnet/minecraftforge/items/wrapper/RecipeWrapper;Lnet/minecraft/world/level/Level;)Z",
            constant = @Constant(intValue = 2), require = 1)
    private int frmc$matchAllInputs(int original, RecipeWrapper inventory, Level level) {
        int size = inventory.getContainerSize();
        return size >= KettleSlots.INVENTORY_SIZE ? KettleSlots.INPUT_COUNT : Math.min(original, size);
    }

    @WrapOperation(
            method = "matches(Lnet/minecraftforge/items/wrapper/RecipeWrapper;Lnet/minecraft/world/level/Level;)Z",
            at = {
                    @At(value = "INVOKE", target = "Lnet/minecraftforge/items/wrapper/RecipeWrapper;getItem(I)Lnet/minecraft/world/item/ItemStack;"),
                    @At(value = "INVOKE", target = "Lnet/minecraftforge/items/wrapper/RecipeWrapper;m_8020_(I)Lnet/minecraft/world/item/ItemStack;")
            }, require = 1)
    private ItemStack frmc$matchInputSlot(RecipeWrapper inventory, int index, Operation<ItemStack> original) {
        int slot = inventory.getContainerSize() >= KettleSlots.INVENTORY_SIZE ? KettleSlots.inventoryInput(index) : index;
        return original.call(inventory, slot);
    }
}
