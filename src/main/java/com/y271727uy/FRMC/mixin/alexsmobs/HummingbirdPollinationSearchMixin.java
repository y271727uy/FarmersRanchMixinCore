package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.ai.HummingbirdAIPollinate;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.alexsmobs.HummingbirdFlowerSections;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(MoveToBlockGoal.class)
public abstract class HummingbirdPollinationSearchMixin {
    @Shadow @Final protected PathfinderMob mob;
    @Shadow @Final private int searchRange;
    @Shadow @Final private int verticalSearchRange;
    @Shadow protected int verticalSearchStart;
    @Unique private HummingbirdFlowerSections frmc$flowerSections;

    @WrapMethod(method = "findNearestBlock", require = 1)
    private boolean frmc$prepareFlowerSearch(Operation<Boolean> original) {
        if (((Object) this).getClass() != HummingbirdAIPollinate.class || !(mob.level() instanceof ServerLevel level)) {
            return original.call();
        }
        var sections = HummingbirdFlowerSections.capture(level, mob.blockPosition(), searchRange,
                verticalSearchStart, verticalSearchRange);
        if (sections != null && !sections.hasCandidates()) {
            return false;
        }
        var previous = frmc$flowerSections;
        frmc$flowerSections = sections;
        try {
            return original.call();
        } finally {
            frmc$flowerSections = previous;
        }
    }

    @WrapOperation(method = "findNearestBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/goal/MoveToBlockGoal;isValidTarget(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"),
            require = 1)
    private boolean frmc$checkFlowerSection(MoveToBlockGoal goal, LevelReader level, BlockPos pos,
                                           Operation<Boolean> original) {
        return (frmc$flowerSections == null || frmc$flowerSections.mayContain(pos))
                && original.call(goal, level, pos);
    }
}
