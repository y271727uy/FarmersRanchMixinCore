package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.event.ServerEvent;
import com.y271727uy.FRMC.integration.delighto_flight.LightningSoilConversion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = ServerEvent.class, remap = false)
public abstract class LightningSoilConversionMixin {
    @Inject(method = "onEntityJoinLevel", at = @At("HEAD"), cancellable = true)
    private static void frmc$avoidBlockingOnNeighborChunks(EntityJoinLevelEvent event, CallbackInfo ci) {
        if (event.getEntity() instanceof LightningBolt bolt && event.getLevel() instanceof ServerLevel level
                && level.getServer().isSameThread()) {
            LightningSoilConversion.apply(level, bolt.blockPosition());
            ci.cancel();
        }
    }
}
