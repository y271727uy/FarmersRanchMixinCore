package com.y271727uy.FRMC.mixin.kaleidoscopecookery;

import com.github.ysbbbbbb.kaleidoscopecookery.entity.ai.CatLieOnBlockGoal;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.computatio.CatFurniturePath;
import com.y271727uy.FRMC.mixin.minecraft.pathfinding.PathNavigationAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(MoveToBlockGoal.class)
public abstract class CatFurnitureNavigationMixin {
    @Shadow @Final protected PathfinderMob mob;

    @WrapOperation(method = {"moveMobToBlock()V", "tick()V"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(DDDD)Z"), require = 2)
    private boolean frmc$findCatFurniturePath(PathNavigation navigation, double x, double y, double z, double speed,
                                              Operation<Boolean> original) {
        if ((Object) this instanceof CatLieOnBlockGoal && mob instanceof Cat cat
                && navigation.getClass() == GroundPathNavigation.class && navigation.isDone()) {
            BlockPos target = BlockPos.containing(x, y, z);
            Path path = CatFurniturePath.find(cat, (GroundPathNavigation) navigation, target);
            if (path != null && navigation.moveTo(path, speed)) {
                PathNavigationAccessor state = (PathNavigationAccessor) navigation;
                state.frmc$setTargetPos(target);
                state.frmc$setReachRange(1);
                state.frmc$resetStuckTimeout();
                return true;
            }
        }
        return original.call(navigation, x, y, z, speed);
    }
}
