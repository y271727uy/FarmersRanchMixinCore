package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.entity.ai.AcquirePoiNoClaim;
import com.cloudmeow.delightoflight.utility.DFUtilities;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.animal.allay.Allay;
import org.apache.commons.lang3.mutable.MutableLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = AcquirePoiNoClaim.class, remap = false)
public abstract class AcquirePoiNoClaimMixin {
    @Inject(method = "lambda$create$1", at = @At("HEAD"), cancellable = true)
    private static void frmc$skipSearchWithoutChefHat(MutableLong nextAttempt, Predicate<?> predicate,
                                                    MemoryAccessor<?, ?> memory, ServerLevel level,
                                                    PathfinderMob mob, long gameTime,
                                                    CallbackInfoReturnable<Boolean> cir) {
        if (mob instanceof Allay && !DFUtilities.checkChefHatExist(mob)) {
            cir.setReturnValue(false);
        }
    }
}
