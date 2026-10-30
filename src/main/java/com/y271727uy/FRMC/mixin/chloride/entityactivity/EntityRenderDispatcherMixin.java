package com.y271727uy.FRMC.mixin.chloride.entityactivity;

import com.y271727uy.FRMC.integration.chloride.ChlorideVisibilityBridge;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes the final client render decision for the existing activity manager. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("RETURN"))
    private <E extends Entity> void frmc$observeFinalDecision(E entity, Frustum frustum,
                                                                double camX, double camY, double camZ,
                                                                CallbackInfoReturnable<Boolean> cir) {
        ChlorideVisibilityBridge.observe(entity, cir.getReturnValue());
    }
}
