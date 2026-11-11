package com.y271727uy.FRMC.mixin.untamed_wilds.util;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import untamedwilds.entity.ComplexMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "untamedwilds.util.EntityDataListenerEvent", remap = false)
public abstract class EntityDataListenerEventMixin {
    @WrapWithCondition(method = "onPlayerLogIn", at = @At(value = "INVOKE", target = "Luntamedwilds/util/EntityDataListenerEvent;registerData()V"), require = 0, expect = 0)
    private static boolean frmc$shouldRegisterData() {
        return ComplexMob.ENTITY_DATA_HASH.isEmpty();
    }
}


