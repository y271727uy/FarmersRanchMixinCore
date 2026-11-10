package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ai.GloomothFindLightGoal;
import com.github.alexmodguy.alexscaves.server.entity.ai.RaycatSitOnBlockGoal;
import com.github.alexmodguy.alexscaves.server.entity.ai.UnderzealotBreakLightGoal;
import com.github.alexmodguy.alexscaves.server.entity.living.BrainiacEntity;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.minecraft.BlockTargetSections;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MoveToBlockGoal.class)
public abstract class CaveBlockTargetSearchMixin {
    @Shadow @Final protected PathfinderMob mob;
    @Shadow @Final private int searchRange;
    @Shadow @Final private int verticalSearchRange;
    @Shadow protected int verticalSearchStart;
    @Unique private BlockTargetSections frmc$caveTargetSections;

    @WrapMethod(method = "findNearestBlock", require = 1)
    private boolean frmc$prepareCaveTargetSearch(Operation<Boolean> original) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return original.call();
        }
        Class<?> goal = getClass();
        Predicate<BlockState> candidate;
        if (goal == RaycatSitOnBlockGoal.class) {
            candidate = state -> state.is(Blocks.CHEST) || state.is(ACTagRegistry.RAYCAT_SLEEPS_ON)
                    || (state.is(Blocks.FURNACE) && state.getValue(FurnaceBlock.LIT)) || state.is(BlockTags.BEDS);
        } else if (goal == GloomothFindLightGoal.class) {
            candidate = state -> state.is(ACTagRegistry.GLOOMOTH_LIGHT_SOURCES);
        } else if (goal == UnderzealotBreakLightGoal.class) {
            candidate = state -> state.is(ACTagRegistry.UNDERZEALOT_LIGHT_SOURCES);
        } else if (mob instanceof BrainiacEntity && goal.getEnclosingClass() == BrainiacEntity.class
                && goal.getSimpleName().equals("PickupBarrelGoal")) {
            var barrel = ACBlockRegistry.WASTE_DRUM.get();
            candidate = state -> state.is(barrel);
        } else {
            return original.call();
        }
        var sections = BlockTargetSections.capture(level, mob.blockPosition(), searchRange,
                verticalSearchStart, verticalSearchRange, candidate);
        if (sections != null && !sections.hasCandidates()) {
            return false;
        }
        var previous = frmc$caveTargetSections;
        frmc$caveTargetSections = sections;
        try {
            return original.call();
        } finally {
            frmc$caveTargetSections = previous;
        }
    }

    @WrapOperation(method = "findNearestBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/goal/MoveToBlockGoal;isValidTarget(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"),
            require = 1)
    private boolean frmc$checkCaveTargetSection(MoveToBlockGoal goal, LevelReader level, BlockPos pos,
            Operation<Boolean> original) {
        return (frmc$caveTargetSections == null || frmc$caveTargetSections.mayContain(pos))
                && original.call(goal, level, pos);
    }
}
