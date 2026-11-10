package com.y271727uy.FRMC.integration.computatio;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public final class FlightFastPath {
    private FlightFastPath() {
    }

    public static boolean canFlyDirectly(ServerLevel level, Mob mob, BlockPos target) {
        if (mob.getPathfindingMalus(BlockPathTypes.OPEN) != 0.0F || mob.isInWaterOrBubble() || mob.isInLava()) {
            return false;
        }

        var sweep = mob.getBoundingBox()
                .expandTowards(DirectNavigationPath.nodePosition(mob, target).subtract(mob.position()));
        BlockPos min = BlockPos.containing(sweep.minX + 1.0E-7, sweep.minY + 1.0E-7, sweep.minZ + 1.0E-7);
        BlockPos max = BlockPos.containing(sweep.maxX - 1.0E-7, sweep.maxY - 1.0E-7, sweep.maxZ - 1.0E-7);
        BlockPos low = min.offset(-1, -1, -1);
        BlockPos high = max.offset(1, 1, 1);
        if (level.isOutsideBuildHeight(low) || level.isOutsideBuildHeight(high)
                || !level.getWorldBorder().isWithinBounds(low) || !level.getWorldBorder().isWithinBounds(high)
                || !level.hasChunksAt(low, high) || level.getBlockCollisions(mob, sweep).iterator().hasNext()) {
            return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.getBlockState(pos).isAir()) {
                return false;
            }
            BlockPathTypes type = WalkNodeEvaluator.getBlockPathTypeStatic(level, pos.mutable());
            if ((type != BlockPathTypes.OPEN && type != BlockPathTypes.WALKABLE)
                    || mob.getPathfindingMalus(type) != 0.0F) {
                return false;
            }
        }
        return true;
    }
}
