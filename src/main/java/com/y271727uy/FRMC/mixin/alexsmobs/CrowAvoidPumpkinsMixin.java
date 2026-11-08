package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityCrow;
import com.y271727uy.FRMC.integration.alexsmobs.CrowFearSearch;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityCrow$AIAvoidPumpkins", remap = false)
public abstract class CrowAvoidPumpkinsMixin {
    @Shadow @Final private int searchLength;
    @Shadow protected BlockPos destinationBlock;
    @Unique private EntityCrow frmc$crow;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void frmc$rememberCrow(EntityCrow crow, CallbackInfo ci) {
        frmc$crow = crow;
    }

    @Inject(method = "searchForDestination", at = @At("HEAD"), cancellable = true)
    private void frmc$findFearBlock(CallbackInfoReturnable<Boolean> cir) {
        if (frmc$crow.level() instanceof ServerLevel level && level.getServer().isSameThread()) {
            BlockPos found = CrowFearSearch.find(level, frmc$crow.blockPosition(), searchLength - 1);
            if (found != null) {
                destinationBlock = found;
            }
            cir.setReturnValue(found != null);
        }
    }
}
