package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityCrow;
import com.y271727uy.FRMC.integration.computatio.CrowDeliveryDetour;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityCrow$AIDepositChests")
public abstract class CrowDepositChestsMixin {
    @Shadow(remap = false) private ItemFrame targetEntity;
    @Unique private EntityCrow frmc$crow;
    @Unique private CrowDeliveryDetour frmc$detour;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void frmc$rememberCrow(EntityCrow crow, CallbackInfo ci) {
        frmc$crow = crow;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/control/MoveControl;setWantedPosition(DDDD)V"))
    private void frmc$flyAroundDeliveryObstacle(MoveControl move, double x, double y, double z,
                                               double speed, Operation<Void> original) {
        if (frmc$detour == null) {
            frmc$detour = new CrowDeliveryDetour();
        }
        Vec3 waypoint = frmc$detour.waypoint(frmc$crow, targetEntity);
        if (waypoint == null) {
            original.call(move, x, y, z, speed);
        } else {
            original.call(move, waypoint.x, waypoint.y, waypoint.z, speed);
        }
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void frmc$discardDeliveryDetour(CallbackInfo ci) {
        if (frmc$detour != null) {
            frmc$detour.reset();
        }
    }
}
