package com.y271727uy.FRMC.mixin.manors_bounty.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.integration.manors_bounty.NestSearchHelper;
import java.util.EnumSet;
import java.util.Set;
import net.mcreator.manors_bounty.block.entity.HayNestBlockEntity;
import net.mcreator.manors_bounty.entity.BresseChickenEntity;
import net.mcreator.manors_bounty.entity.MallardDuckEntity;
import net.mcreator.manors_bounty.entity.TurkeyEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = {
        BresseChickenEntity.FindNearestNestGoal.class,
        MallardDuckEntity.FindNearestNestGoal.class,
        TurkeyEntity.FindNearestNestGoal.class
}, remap = false)
public abstract class PoultryNestGoalMixin extends Goal {
    @Shadow(remap = false)
    private BlockPos targetNest;

    @Shadow(remap = false)
    @Final
    private double speed;

    @Shadow(remap = false)
    private int searchCooldown;

    @Unique
    private Mob frmc$poultry;

    @Unique
    private Set<BlockPos> frmc$nestCandidates;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void frmc$claimMovement(@Coerce Mob poultry, double speed, CallbackInfo ci) {
        this.frmc$poultry = poultry;
        this.searchCooldown = NestSearchHelper.searchStagger(poultry);
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Inject(method = "findNearestNest()Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void frmc$collectAvailableNests(CallbackInfoReturnable<BlockPos> cir) {
        this.frmc$nestCandidates = NestSearchHelper.findAvailableNests(this.frmc$poultry);
        BlockPos candidate = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (BlockPos nest : this.frmc$nestCandidates) {
            double distance = this.frmc$nestDistanceSquared(nest);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                candidate = nest;
            }
        }
        if (candidate == null) {
            this.frmc$nestCandidates = null;
            this.searchCooldown = NestSearchHelper.failedSearchCooldown(this.frmc$poultry);
        }
        cir.setReturnValue(candidate);
    }

    @Inject(method = "moveToNest()V", at = @At("HEAD"), cancellable = true)
    private void frmc$submitPathOnce(CallbackInfo ci) {
        ci.cancel();
        Set<BlockPos> candidates = this.frmc$nestCandidates;
        this.frmc$nestCandidates = null;
        if (candidates == null) {
            return;
        }
        this.searchCooldown += NestSearchHelper.searchStagger(this.frmc$poultry);
        if (this.frmc$isWithinLayingDistance(this.targetNest)) {
            return;
        }

        PathNavigation navigation = this.frmc$poultry.getNavigation();
        // Accuracy 1 accepts an adjacent block, which can be outside the laying radius.
        Path path = navigation.createPath(Set.of(this.targetNest), 0);
        if ((path == null || !path.canReach()) && candidates.size() > 1) {
            // Keep a failed nearest nest from drawing the second search away from the alternatives.
            candidates.remove(this.targetNest);
            path = navigation.createPath(candidates, 0);
        }
        if (path != null && path.canReach() && navigation.moveTo(path, this.speed)) {
            this.targetNest = path.getTarget();
        } else {
            this.stop();
        }
    }

    @Inject(method = "canContinueToUse()Z", at = @At("HEAD"), cancellable = true, remap = true)
    private void frmc$releaseUnusableNest(CallbackInfoReturnable<Boolean> cir) {
        if (this.targetNest == null || !NestSearchHelper.isAvailableNest(this.frmc$poultry, this.targetNest)) {
            cir.setReturnValue(false);
            return;
        }
        Path path = this.frmc$poultry.getNavigation().getPath();
        if (!this.frmc$isWithinLayingDistance(this.targetNest)
                && (path == null || path.isDone() || !path.canReach() || !this.targetNest.equals(path.getTarget()))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "teleportAndForceLay()V", at = @At("HEAD"), cancellable = true)
    private void frmc$checkVacancyBeforeLaying(CallbackInfo ci) {
        if (!NestSearchHelper.isAvailableNest(this.frmc$poultry, this.targetNest)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean frmc$isWithinLayingDistance(BlockPos nest) {
        return this.frmc$nestDistanceSquared(nest) < 1.0;
    }

    @Unique
    private double frmc$nestDistanceSquared(BlockPos nest) {
        return this.frmc$poultry.distanceToSqr(nest.getX() + 0.5, nest.getY(), nest.getZ() + 0.5);
    }

    @WrapOperation(method = "teleportAndForceLay()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;sendBlockUpdated(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;I)V",
            remap = true))
    private void frmc$skipDuplicateNestUpdate(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState,
                                              int flags, Operation<Void> original, @Local BlockEntity nest) {
        // The original call to HayNestBlockEntity.setChanged already sends the update.
        if (nest.getClass() != HayNestBlockEntity.class) {
            original.call(level, pos, oldState, newState, flags);
        }
    }
}
