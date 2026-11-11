package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.entity.PhantomMembraneBalloonEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = PhantomMembraneBalloonEntity.class, remap = false)
public abstract class PhantomMembraneBalloonCargoMixin {
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), remap = true)
    private void frmc$restoreLegacyCargo(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains("Products", Tag.TAG_LIST)) {
            PhantomMembraneBalloonEntity balloon = (PhantomMembraneBalloonEntity) (Object) this;
            balloon.addStoredProduct(ItemStack.of(tag.getCompound("AstralBerries")));
            balloon.addStoredProduct(ItemStack.of(tag.getCompound("Stardrops")));
        }
    }
}
