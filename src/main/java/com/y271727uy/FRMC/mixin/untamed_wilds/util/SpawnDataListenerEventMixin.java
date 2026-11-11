package com.y271727uy.FRMC.mixin.untamed_wilds.util;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;
import java.util.List;

@Pseudo
@Mixin(targets = "untamedwilds.util.SpawnDataListenerEvent", remap = false)
public abstract class SpawnDataListenerEventMixin {
    @WrapOperation(method = "registerSpawnData", at = @At(value = "INVOKE", target = "Ljava/util/List;addAll(Ljava/util/Collection;)Z"), require = 0, expect = 0)
    private static <E> boolean frmc$replaceDuplicateSpawnListAppend(List<E> list, Collection<? extends E> entries, Operation<Boolean> original) {
        if (!list.isEmpty()) {
            list.clear();
        }
        return original.call(list, entries);
    }
}


