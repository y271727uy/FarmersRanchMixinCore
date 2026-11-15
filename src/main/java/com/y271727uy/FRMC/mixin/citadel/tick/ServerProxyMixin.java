package com.y271727uy.FRMC.mixin.citadel.tick;

import com.github.alexthe666.citadel.ServerProxy;
import com.github.alexthe666.citadel.server.tick.ServerTickRateTracker;
import com.y271727uy.FRMC.integration.citadel.tick.CitadelDataCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** canEntityTickServer 对每个实体每 tick 调用；没有任何 tick 速率修改时直接放行。 */
@Pseudo
@Mixin(value = ServerProxy.class, remap = false)
public abstract class ServerProxyMixin {
    @Inject(method = "canEntityTickServer", at = @At("HEAD"), cancellable = true)
    private void frmc$fastEntityTickCheck(Level level, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel serverLevel)) {
            cir.setReturnValue(true);
            return;
        }
        ServerTickRateTracker tracker = CitadelDataCache.trackerFor(serverLevel.getServer());
        if (tracker.tickRateModifierList.isEmpty() && tracker.specialTickRateEntities.isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}
