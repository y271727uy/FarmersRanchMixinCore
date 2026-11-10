package com.y271727uy.FRMC.mixin.delighto_flight;

import com.bawnorton.mixinsquared.TargetHandler;
import com.y271727uy.FRMC.integration.delighto_flight.ProjectileDodgeState;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Projectile.class, priority = 1500)
public abstract class ProjectileDodgeMixin implements ProjectileDodgeState {
    @Unique private HitResult frmc$checkedHit;

    @Override
    public void frmc$markDodgeChecked(HitResult hit) {
        this.frmc$checkedHit = hit;
    }

    @Dynamic("DelightoFlight projectile hit handler")
    @TargetHandler(mixin = "com.cloudmeow.delightoflight.mixin.ProjectileMixin", name = "onHit")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void frmc$skipLateDodge(HitResult hit, CallbackInfo projectileHit, CallbackInfo handler) {
        // Custom projectiles can call onHit without posting Forge's impact event.
        if (hit == this.frmc$checkedHit || ((Projectile) (Object) this).level().isClientSide) {
            handler.cancel();
        }
        this.frmc$checkedHit = null;
    }
}
