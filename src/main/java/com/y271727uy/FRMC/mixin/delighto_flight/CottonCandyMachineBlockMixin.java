package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.block.CottonCandyMachineBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = CottonCandyMachineBlock.class, remap = false)
public abstract class CottonCandyMachineBlockMixin {
    @WrapOperation(method = "use", remap = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean frmc$returnOrDropIngredients(Inventory inventory, ItemStack stack, Operation<Boolean> original) {
        boolean inserted = original.call(inventory, stack);
        if (!stack.isEmpty()) {
            inventory.player.drop(stack, false);
        }
        return inserted;
    }
}
