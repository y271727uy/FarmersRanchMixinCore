package com.y271727uy.FRMC.mixin.alcocraftplus;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import umpaz.brewinandchewin.common.registry.BnCItems;

@Pseudo
@Mixin(targets = "me.itzme1on.alcocraftplus.core.items.MugItem", remap = false)
public abstract class MugItemMixin {
    @ModifyExpressionValue(method = {"finishUsingItem", "m_5922_"}, at = @At(value = "NEW",
        target = "(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack frmc$returnTankardAfterDrinking(ItemStack originalMug) {
        if (originalMug.getItem() == (Object) this) {
            return originalMug;
        }
        return new ItemStack(BnCItems.TANKARD.get());
    }
}
