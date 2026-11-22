package com.y271727uy.FRMC.mixin.kaleidoscopetavern;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BarCabinetBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BottleBlock;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.y271727uy.FRMC.capability.displaywine.DisplayWineBottles;
import com.y271727uy.FRMC.capability.displaywine.DisplayWinePlaceholders;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BarCabinetBlock.class, remap = false)
public abstract class BarCabinetBlockMixin {
    @ModifyReturnValue(method = "getBottleBlock", at = @At("RETURN"), require = 0)
    private BottleBlock frmc$allowTaggedBottles(BottleBlock original, ItemStack stack) {
        if (original != null) {
            return original;
        }
        if (DisplayWineBottles.isBottle(stack)) {
            return DisplayWinePlaceholders.placeholder(stack);
        }
        return original;
    }
}
