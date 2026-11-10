package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.compat.dungeonsdelight.DDCompat;
import com.cloudmeow.delightoflight.compat.goetydelight.GDCompat;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = {DDCompat.class, GDCompat.class}, remap = false)
public abstract class CompatiblePotIngredientMixin {
    // 与提交pr重合
    @WrapWithCondition(method = {"addIngredientForDDPot", "addIngredientForGDPot"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;putInt(Ljava/lang/String;I)V",
                    remap = true))
    private static boolean frmc$advanceAfterInsertion(CompoundTag tag, String key, int slot,
                                                       @Local(argsOnly = true) LivingEntity allay) {
        return allay.getMainHandItem().isEmpty();
    }
}
