package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.entity.ai.PutItemIntoOtherPot;
import com.cloudmeow.delightoflight.entity.ai.PutItemIntoPot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

@Pseudo
@Mixin(value = {PutItemIntoPot.class, PutItemIntoOtherPot.class}, remap = false)
public abstract class PutItemIntoPotMixin {
    // 与提交pr重合
    @Inject(method = "addIngredient", at = @At("HEAD"), cancellable = true)
    private void frmc$insertWithoutDecodingBook(CookingPotBlockEntity pot, LivingEntity entity, CallbackInfo ci) {
        ci.cancel();
        ItemStack held = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        if (held.isEmpty()) {
            return;
        }
        ItemStack book = entity.getItemBySlot(EquipmentSlot.CHEST);
        CompoundTag tag = book.getTag();
        if (book.isEmpty() || !book.hasTag() || tag == null || !tag.contains("Items", Tag.TAG_COMPOUND)) {
            return;
        }
        int count = tag.getCompound("Items").getList("Items", Tag.TAG_COMPOUND).size();
        int slot = tag.getInt("Slot");
        if (slot >= 0 && slot < count) {
            ItemStack leftover = pot.getInventory().insertItem(slot, held, false);
            entity.setItemSlot(EquipmentSlot.MAINHAND, leftover);
            if (leftover.isEmpty()) {
                tag.putInt("Slot", slot + 1 < count ? slot + 1 : 0);
            }
        }
    }
}
