package com.y271727uy.FRMC.mixin.kaleidoscopetavern;

import com.github.ysbbbbbb.kaleidoscopetavern.item.ShakerItem;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.ShakerIngredients;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShakerItem.class, remap = false)
public abstract class ShakerItemMixin {
    @ModifyArg(method = "getStorage", at = @At(value = "INVOKE", target =
            "Lnet/minecraftforge/items/ItemStackHandler;deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V"), index = 0)
    private static CompoundTag frmc$expandSavedShakerItem(CompoundTag storage) {
        return ShakerIngredients.expandStorage(storage);
    }

    @Inject(method = {"use", "m_7203_"}, at = @At(value = "INVOKE", target =
            "Lcom/github/ysbbbbbb/kaleidoscopetavern/item/ShakerItem;getStorage(Lnet/minecraft/world/item/ItemStack;)"
                    + "Lnet/minecraftforge/items/ItemStackHandler;"), cancellable = true)
    private void frmc$shakeWithAnyValidCount(Level level, Player player, InteractionHand hand,
                                            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack shaker = player.getItemInHand(hand);
        if (ShakerIngredients.hasEnoughToShake(ShakerItem.getStorage(shaker))) {
            player.startUsingItem(hand);
            cir.setReturnValue(InteractionResultHolder.consume(shaker));
        }
    }
}
