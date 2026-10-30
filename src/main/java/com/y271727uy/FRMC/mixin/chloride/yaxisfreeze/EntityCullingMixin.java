package com.y271727uy.FRMC.mixin.chloride.yaxisfreeze;

import com.y271727uy.FRMC.integration.chloride.ChlorideVisibilityBridge;
import me.srrapero720.chloride.impl.EntityCulling;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hooks Chloride's own entity-distance decision, including its configured Y range. */
@Mixin(value = EntityCulling.class, remap = false)
public abstract class EntityCullingMixin {
    @Inject(method = "isEntityInRange(Lnet/minecraft/world/entity/Entity;DDDII)Z", at = @At("RETURN"))
    private static void frmc$observeYAxisDecision(Entity entity, double cameraX, double cameraY,
                                                   double cameraZ, int distanceY, int distanceX,
                                                   CallbackInfoReturnable<Boolean> cir) {
        ChlorideVisibilityBridge.observeYAxis(entity, cir.getReturnValue());
    }
}
