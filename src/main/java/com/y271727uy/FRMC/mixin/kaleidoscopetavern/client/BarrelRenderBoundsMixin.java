package com.y271727uy.FRMC.mixin.kaleidoscopetavern.client;

import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.BarrelBlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BarrelBlockEntity.class, remap = false)
public abstract class BarrelRenderBoundsMixin {
    @Inject(method = "getRenderBoundingBox", at = @At("RETURN"), cancellable = true, require = 0)
    private void frmc$includeOpenLid(CallbackInfoReturnable<AABB> cir) {
        BarrelBlockEntity barrel = (BarrelBlockEntity) (Object) this;
        if (!barrel.isOpen()) {
            return;
        }

        // The open lid reaches 3.99 blocks above the bottom-center anchor.
        double lidCeiling = barrel.getBlockPos().getY() + 4.0;
        AABB bounds = cir.getReturnValue();
        if (bounds.maxY < lidCeiling) {
            cir.setReturnValue(bounds.expandTowards(0.0, lidCeiling - bounds.maxY, 0.0));
        }
    }
}
