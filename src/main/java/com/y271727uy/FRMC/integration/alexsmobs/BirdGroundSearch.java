package com.y271727uy.FRMC.integration.alexsmobs;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class BirdGroundSearch {
    private static final int ORIGINAL_FLOOR = -64;
    private static final int ORIGINAL_FLUID_CEILING = 320;
    private static final Predicate<BlockState> DRY = state -> state.getFluidState().isEmpty();
    private static final Predicate<BlockState> SOLID = BlockState::isSolid;
    private static final Predicate<BlockState> SOLID_OR_FLUID = state -> state.isSolid() || !DRY.test(state);
    private static final Predicate<BlockState> NOT_AIR = state -> !state.isAir();

    private BirdGroundSearch() {
    }

    public static BlockPos find(Entity bird, BlockPos column, boolean riseOutOfFluid, boolean stopAtFluid) {
        if (!(bird.level() instanceof ServerLevel level) || !level.getServer().isSameThread()) {
            return null;
        }
        int x = column.getX();
        int z = column.getZ();
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) {
            return null;
        }
        // Alex's Mobs truncates the bird's Y and uses fixed scan limits.
        int y = (int) bird.getY();
        int localX = x & 15;
        int localZ = z & 15;
        if (riseOutOfFluid) {
            y = rise(level, chunk, localX, y, localZ);
        }
        y = descend(level, chunk, localX, y, localZ, ORIGINAL_FLOOR, stopAtFluid ? SOLID_OR_FLUID : SOLID);
        return new BlockPos(x, y, z);
    }

    /** Advances an air-only vanilla scan to the first non-air block or its original lower limit. */
    public static BlockPos skipAir(Level world, BlockPos start, int lowerLimit) {
        if (start.getY() <= lowerLimit || !(world instanceof ServerLevel level) || !level.getServer().isSameThread()) {
            return start;
        }
        int x = start.getX();
        int z = start.getZ();
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) {
            return start;
        }
        int y = descend(level, chunk, x & 15, start.getY(), z & 15, lowerLimit, NOT_AIR);
        return y == start.getY() ? start : new BlockPos(x, y, z);
    }

    private static int rise(ServerLevel level, LevelChunk chunk, int x, int y, int z) {
        int ceiling = Math.min(ORIGINAL_FLUID_CEILING, level.getMaxBuildHeight());
        while (y >= level.getMinBuildHeight() && y < ceiling) {
            var section = chunk.getSection(level.getSectionIndex(y));
            if (DRY.test(section.getBlockState(x, y & 15, z))) {
                break;
            }
            int end = Math.min(ceiling, (y & ~15) + 16);
            y++;
            if (!section.maybeHas(DRY)) {
                y = end;
            }
            while (y < end && !DRY.test(section.getBlockState(x, y & 15, z))) {
                y++;
            }
            if (y < end) {
                break;
            }
        }
        return y;
    }

    private static int descend(ServerLevel level, LevelChunk chunk, int x, int y, int z, int lowerLimit,
                               Predicate<BlockState> stop) {
        if (y <= lowerLimit) {
            return y;
        }
        y = Math.min(y, level.getMaxBuildHeight() - 1);
        int floor = Math.max(lowerLimit + 1, level.getMinBuildHeight());
        while (y >= floor) {
            var section = chunk.getSection(level.getSectionIndex(y));
            if (stop.test(section.getBlockState(x, y & 15, z))) {
                return y;
            }
            int end = Math.max(floor, y & ~15);
            y--;
            if (!section.maybeHas(stop)) {
                y = end - 1;
            }
            while (y >= end && !stop.test(section.getBlockState(x, y & 15, z))) {
                y--;
            }
            if (y >= end) {
                return y;
            }
        }
        return lowerLimit;
    }
}
