package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.block.entity.CottonCandyMachineBlockEntity;
import com.y271727uy.FRMC.integration.delighto_flight.CottonCandyHudState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = CottonCandyMachineBlockEntity.class, remap = false)
public abstract class CottonCandyMachineClientStateMixin {
    @Inject(method = {"load(Lnet/minecraft/nbt/CompoundTag;)V", "m_142466_(Lnet/minecraft/nbt/CompoundTag;)V"},
            at = @At("TAIL"), require = 1)
    private void frmc$trackSyncedSpinState(CallbackInfo ci) {
        CottonCandyHudState.onStateLoaded((CottonCandyMachineBlockEntity) (Object) this);
    }
}
