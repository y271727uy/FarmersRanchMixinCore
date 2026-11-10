package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.crafting.CottonCandyMachineRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = CottonCandyMachineRecipe.class, remap = false)
public abstract class CottonCandyMachineRecipeMixin {
    @Shadow @Final private NonNullList<Ingredient> inputItems;

    // 与提交pr重合
    @Inject(method = "matches(Lnet/minecraftforge/items/wrapper/RecipeWrapper;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true)
    private void frmc$matchWithoutCopyingSlots(RecipeWrapper input, Level level, CallbackInfoReturnable<Boolean> cir) {
        int size = input.getContainerSize();
        if (size > CottonCandyMachineRecipe.INPUT_SLOTS) {
            return;
        }
        for (Ingredient ingredient : inputItems) {
            if (!ingredient.isVanilla()) {
                return;
            }
        }

        // Follow DF's first-match order and consume each slot at most once.
        int usedSlots = 0;
        for (Ingredient ingredient : inputItems) {
            if (ingredient.isEmpty()) {
                continue;
            }
            int index = 0;
            for (; index < size; index++) {
                int bit = 1 << index;
                if ((usedSlots & bit) == 0) {
                    ItemStack stack = input.getItem(index);
                    if (!stack.isEmpty() && ingredient.test(stack)) {
                        usedSlots |= bit;
                        break;
                    }
                }
            }
            if (index == size) {
                cir.setReturnValue(false);
                return;
            }
        }
        cir.setReturnValue(true);
    }
}
