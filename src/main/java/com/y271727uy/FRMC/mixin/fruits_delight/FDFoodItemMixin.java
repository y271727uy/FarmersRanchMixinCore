package com.y271727uy.FRMC.mixin.fruits_delight;

import com.y271727uy.FRMC.util.FruitsDelightRemainderHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "dev.xkmc.fruitsdelight.content.item.FDFoodItem", remap = false)
public abstract class FDFoodItemMixin extends Item {
    protected FDFoodItemMixin(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return FruitsDelightRemainderHelper.replaceGlassBottleRemainder(stack, super.getCraftingRemainingItem(stack));
    }
}



