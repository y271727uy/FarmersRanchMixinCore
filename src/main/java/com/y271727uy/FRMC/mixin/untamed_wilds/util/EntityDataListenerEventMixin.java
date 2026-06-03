package com.y271727uy.FRMC.mixin.untamed_wilds.util;

import untamedwilds.entity.ComplexMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "untamedwilds.util.EntityDataListenerEvent", remap = false)
public abstract class EntityDataListenerEventMixin {
    @Redirect(method = "onPlayerLogIn", at = @At(value = "INVOKE", target = "Luntamedwilds/util/EntityDataListenerEvent;registerData()V"), require = 0, expect = 0)
    private static void frmc$skipRedundantRegisterData() {
        if (ComplexMob.ENTITY_DATA_HASH.isEmpty()) {
            untamedwilds.util.EntityDataListenerEvent.registerData();
        }
    }
}


