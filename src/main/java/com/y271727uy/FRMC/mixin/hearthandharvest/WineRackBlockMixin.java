package com.y271727uy.FRMC.mixin.hearthandharvest;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.capability.displaywine.DisplayWineBottles;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "alabaster.hearthandharvest.common.block.WineRackBlock", remap = false)
public abstract class WineRackBlockMixin {
    @WrapOperation(method = {"use", "m_6227_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;", remap = true))
    private Item frmc$allowPlaceableBottles(ItemStack stack, Operation<Item> original) {
        Item item = original.call(stack);
        // The rack uses this value only for its BlockItem rejection check.
        return item instanceof BlockItem && DisplayWineBottles.isBottle(stack) ? Items.GLASS_BOTTLE : item;
    }
}
