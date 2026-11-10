package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.entity.ai.GetItemFromContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = GetItemFromContainer.class, remap = false)
public abstract class GetItemFromContainerMixin {
    @Inject(method = "getIngredient", at = @At("HEAD"), cancellable = true)
    private void frmc$readOnlyRequestedIngredient(Container basket, LivingEntity entity, CallbackInfo ci) {
        ci.cancel();
        if (!entity.getMainHandItem().isEmpty()) {
            return;
        }
        ItemStack book = entity.getItemBySlot(EquipmentSlot.CHEST);
        CompoundTag tag = book.getTag();
        if (book.isEmpty() || !book.hasTag() || tag == null || !tag.contains("Items", Tag.TAG_COMPOUND)) {
            return;
        }
        ListTag ingredients = tag.getCompound("Items").getList("Items", Tag.TAG_COMPOUND);
        int slot = tag.getInt("Slot");
        if (slot < 0 || slot >= ingredients.size()) {
            return;
        }

        // DF indexes the serialized list, which omits empty recipe slots.
        ItemStack ingredient = ItemStack.of(ingredients.getCompound(slot));
        for (int index = 0; index < basket.getContainerSize(); index++) {
            ItemStack stack = basket.getItem(index);
            if (ItemStack.isSameItem(ingredient, stack)) {
                ItemStack taken = basket.removeItem(index, 1);
                if (!taken.isEmpty()) {
                    basket.setChanged();
                    entity.setItemInHand(InteractionHand.MAIN_HAND, taken);
                    return;
                }
            }
        }
    }
}
