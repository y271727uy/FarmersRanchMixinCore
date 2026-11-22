package com.y271727uy.FRMC.mixin.kaleidoscopetavern;

import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.mixology.ShakerBlockEntity;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.ShakerIngredients;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = ShakerBlockEntity.class, remap = false)
public abstract class ShakerBlockEntityMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 3))
    private int frmc$expandShakerSlots(int original) {
        return ShakerIngredients.MAX_INPUTS;
    }

    @ModifyArg(method = {"load", "m_142466_"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraftforge/items/ItemStackHandler;deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V"), index = 0)
    private CompoundTag frmc$expandSavedShaker(CompoundTag storage) {
        return ShakerIngredients.expandStorage(storage);
    }
}
