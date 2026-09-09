package com.y271727uy.FRMC.mixin.untamed_wilds;

import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import untamedwilds.util.SpeciesDataHolder;

@Pseudo
@Mixin(targets = "untamedwilds.util.EntityDataHolder", remap = false)
public abstract class EntityDataHolderMixin {
    @Shadow @Final private String name;
    @Shadow @Final private List<SpeciesDataHolder> speciesData;

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true, require = 0)
    private void frmc$returnBaseNameWhenSpeciesAreMissing(int species, CallbackInfoReturnable<String> cir) {
        if (speciesData.isEmpty() || species < 0 || species >= speciesData.size()) {
            cir.setReturnValue(name);
        }
    }
}
