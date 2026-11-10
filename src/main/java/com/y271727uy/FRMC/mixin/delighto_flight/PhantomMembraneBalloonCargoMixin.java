package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.entity.PhantomMembraneBalloonEntity;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = PhantomMembraneBalloonEntity.class, remap = false)
public abstract class PhantomMembraneBalloonCargoMixin {
    @Shadow @Final private List<ItemStack> storedProducts;

    // 与提交pr重合
    @Inject(method = "addStoredProduct", at = @At("HEAD"), cancellable = true)
    private void frmc$storeLegalStacks(ItemStack stack, CallbackInfo ci) {
        ci.cancel();
        frmc$addCargo(stack);
    }

    // 与提交pr重合
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), remap = true)
    private void frmc$normalizeSavedCargo(CompoundTag tag, CallbackInfo ci) {
        List<ItemStack> saved = List.copyOf(this.storedProducts);
        this.storedProducts.clear();
        saved.forEach(this::frmc$addCargo);
        if (!tag.contains("Products", Tag.TAG_LIST)) {
            frmc$addCargo(ItemStack.of(tag.getCompound("AstralBerries")));
            frmc$addCargo(ItemStack.of(tag.getCompound("Stardrops")));
        }
    }

    @Unique
    private void frmc$addCargo(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        int remaining = stack.getCount();
        // ItemStack.save writes Count as a signed byte in Minecraft 1.20.1.
        int maxCount = Math.min(Byte.MAX_VALUE, stack.getMaxStackSize());
        for (ItemStack stored : this.storedProducts) {
            if (!ItemStack.isSameItemSameTags(stored, stack)) {
                continue;
            }
            int count = Math.min(remaining, maxCount - stored.getCount());
            stored.grow(count);
            remaining -= count;
            if (remaining == 0) {
                return;
            }
        }
        while (remaining > 0) {
            int count = Math.min(remaining, maxCount);
            this.storedProducts.add(stack.copyWithCount(count));
            remaining -= count;
        }
    }
}
