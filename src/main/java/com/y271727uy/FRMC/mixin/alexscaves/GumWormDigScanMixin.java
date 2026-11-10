package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.living.GumWormEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GumWormEntity.class)
public abstract class GumWormDigScanMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", remap = false, target =
            "Lcom/github/alexmodguy/alexscaves/server/entity/living/GumWormEntity;isDigLogic()Z"),
            require = 1)
    private boolean frmc$skipDigScanWhileLeaping(GumWormEntity worm, Operation<Boolean> original) {
        return !worm.isLeaping() && original.call(worm);
    }
}
