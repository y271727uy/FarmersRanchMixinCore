package com.y271727uy.FRMC.mixin.vinery;

import com.y271727uy.FRMC.integration.vinery.VineryAgeing;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.satisfy.vinery.client.gui.handler.FermentationBarrelGuiHandler", remap = false)
public abstract class FermentationBarrelMenuAgeingMixin {
    @Inject(method = "isIngredient", at = @At("HEAD"), cancellable = true)
    private void frmc$acceptAgeingItems(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (VineryAgeing.accepts(stack)) {
            cir.setReturnValue(true);
        }
    }

    @WrapOperation(method = {"quickMoveStack", "m_7648_"}, at = @At(value = "INVOKE",
        target = "Lnet/satisfy/vinery/core/util/JuiceUtil;isJuice(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean frmc$routeAgeingJuiceToIngredientSlots(ItemStack stack, Operation<Boolean> original) {
        return !VineryAgeing.accepts(stack) && original.call(stack);
    }

    @WrapOperation(method = {"quickMoveStack", "m_7648_"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z", remap = true))
    private boolean frmc$routeAgeingBottlesToIngredientSlots(ItemStack stack, Item item, Operation<Boolean> original) {
        return !VineryAgeing.accepts(stack) && original.call(stack, item);
    }
}
