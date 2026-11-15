package com.y271727uy.FRMC.mixin.citadel.tick;

import com.github.alexthe666.citadel.client.tick.ClientTickRateTracker;
import com.github.alexthe666.citadel.server.tick.TickRateTracker;
import com.y271727uy.FRMC.integration.citadel.tick.ClientTrackerCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = ClientTickRateTracker.class, remap = false)
public abstract class ClientTickRateTrackerMixin {
    @Inject(method = "getForClient", at = @At("HEAD"), cancellable = true)
    private static void frmc$getCached(Minecraft minecraft, CallbackInfoReturnable<ClientTickRateTracker> cir) {
        ClientTickRateTracker cached = ClientTrackerCache.lookup(minecraft);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "getForClient", at = @At("RETURN"))
    private static void frmc$storeInCache(Minecraft minecraft, CallbackInfoReturnable<ClientTickRateTracker> cir) {
        ClientTickRateTracker tracker = cir.getReturnValue();
        if (tracker != null) {
            ClientTrackerCache.store(minecraft, tracker);
        }
    }

    @Inject(method = "modifySoundPitch", at = @At("HEAD"), cancellable = true)
    private void frmc$fastSoundPitch(SoundInstance soundInstance, CallbackInfoReturnable<Float> cir) {
        if (frmc$noModifiers()) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "getClientTickRate", at = @At("HEAD"), cancellable = true)
    private void frmc$fastClientTickRate(CallbackInfoReturnable<Float> cir) {
        if (frmc$noModifiers()) {
            cir.setReturnValue(50.0F);
        }
    }

    @Unique
    private boolean frmc$noModifiers() {
        return ((TickRateTracker) (Object) this).tickRateModifierList.isEmpty();
    }
}
