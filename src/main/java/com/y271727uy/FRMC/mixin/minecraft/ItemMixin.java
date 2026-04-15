package com.y271727uy.FRMC.mixin.minecraft;

import com.y271727uy.FRMC.util.FruitsDelightRemainderHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {
    @Inject(method = "getCraftingRemainingItem", at = @At("RETURN"), cancellable = true)
    private void frmc$replaceFruitsDelightRemainder(CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(FruitsDelightRemainderHelper.replaceGlassBottleRemainder((Item) (Object) this, cir.getReturnValue()));
    }

    @Inject(method = "hasCraftingRemainingItem", at = @At("RETURN"), cancellable = true)
    private void frmc$replaceFruitsDelightRemainderPresence(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(FruitsDelightRemainderHelper.hasReplacementRemainder((Item) (Object) this, cir.getReturnValueZ()));
    }
}

