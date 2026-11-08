package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.entity.MoonSlashEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = MoonSlashEntity.class, remap = false)
public abstract class MoonSlashEntityMixin {
    @WrapOperation(method = "getEffectAmplifier", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/effect/MobEffectInstance;getAmplifier()I", remap = true))
    private int frmc$useBaseDamageWhenEffectExpires(MobEffectInstance effect, Operation<Integer> original) {
        return effect == null ? 0 : original.call(effect);
    }
}
