package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.block.blockentity.NuclearFurnaceBlockEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = NuclearFurnaceBlockEntity.class, remap = false)
public abstract class NuclearFurnaceSyncMixin {
    @Shadow private int cookTime;
    @Shadow private int maxCookTime;
    @Shadow private int fissionTime;
    @Shadow private int barrelTime;
    @Shadow private int currentWaste;
    @Unique private boolean frmc$lastSyncWasIdle;
    @Unique private int frmc$lastIdleWaste;
    @Unique private int frmc$lastIdleMaxCookTime;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target =
            "Lcom/github/alexmodguy/alexscaves/server/block/blockentity/NuclearFurnaceBlockEntity;syncWithClient()V"),
            require = 1)
    private static void frmc$syncChangedFurnace(NuclearFurnaceBlockEntity furnace, Operation<Void> original) {
        var state = (NuclearFurnaceSyncMixin) (Object) furnace;
        boolean idle = state.cookTime == 0 && state.fissionTime == 0 && state.barrelTime == 0;
        if (idle && state.frmc$lastSyncWasIdle && state.frmc$lastIdleWaste == state.currentWaste
                && state.frmc$lastIdleMaxCookTime == state.maxCookTime) {
            return;
        }
        original.call(furnace);
        state.frmc$lastSyncWasIdle = idle;
        state.frmc$lastIdleWaste = state.currentWaste;
        state.frmc$lastIdleMaxCookTime = state.maxCookTime;
    }
}
