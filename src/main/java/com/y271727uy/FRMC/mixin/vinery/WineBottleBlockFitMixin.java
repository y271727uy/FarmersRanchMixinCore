package com.y271727uy.FRMC.mixin.vinery;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.y271727uy.FRMC.capability.displaywine.DisplayWineBottles;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.satisfy.vinery.core.block.WineBottleBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = WineBottleBlock.class, remap = false)
public abstract class WineBottleBlockFitMixin {
    @ModifyReturnValue(method = "willFitStack", at = @At("RETURN"), require = 0)
    private boolean frmc$allowTaggedBottles(boolean original, ItemStack stack, NonNullList<ItemStack> inventory) {
        return original || DisplayWineBottles.isSmallBottle(stack);
    }
}
