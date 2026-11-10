package com.y271727uy.FRMC.mixin.alexscaves;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.github.alexmodguy.alexscaves.server.entity.living.GammaroachEntity$MeleeGoal")
public abstract class GammaroachMeleeGoalMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", remap = false,
            target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"), require = 1)
    private void frmc$putClosestFirst(List<Mob> candidates, Comparator<? super Mob> byDistance, Operation<Void> original) {
        if (candidates.size() < 2) {
            return;
        }
        var iterator = candidates.listIterator();
        Mob closest = iterator.next();
        int closestIndex = 0;
        while (iterator.hasNext()) {
            int index = iterator.nextIndex();
            Mob candidate = iterator.next();
            if (byDistance.compare(candidate, closest) < 0) {
                closest = candidate;
                closestIndex = index;
            }
        }
        if (closestIndex != 0) {
            Collections.swap(candidates, 0, closestIndex);
        }
    }
}
