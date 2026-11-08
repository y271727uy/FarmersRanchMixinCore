package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.entity.ai.aerolope.PrepareRamNearestGoat;
import com.cloudmeow.delightoflight.registry.DFMemoryTypes;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.goat.Goat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = PrepareRamNearestGoat.class, remap = false)
public abstract class PrepareRamNearestGoatMixin {
    @Shadow @Final private TargetingConditions ramTargeting;

    @Shadow
    private void chooseRamPosition(PathfinderMob entity, Goat target) {
        throw new AssertionError();
    }

    @Inject(method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)V",
            at = @At("HEAD"), cancellable = true)
    private void frmc$checkOnlyCloserTargets(ServerLevel level, PathfinderMob entity, long gameTime, CallbackInfo ci) {
        List<Goat> candidates = entity.getBrain().getMemory(DFMemoryTypes.NEAREST_GOAT.get()).orElse(List.of());
        Goat nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (Goat candidate : candidates) {
            double distance = candidate.distanceToSqr(entity);
            // Retain DF's sensor order for ties. Farther candidates cannot win even if visible.
            if ((nearest == null || Double.compare(distance, nearestDistance) < 0) && ramTargeting.test(entity, candidate)) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        if (nearest != null) {
            chooseRamPosition(entity, nearest);
        }
        ci.cancel();
    }
}
