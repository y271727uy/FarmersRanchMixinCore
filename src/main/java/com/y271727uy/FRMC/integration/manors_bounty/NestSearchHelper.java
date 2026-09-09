package com.y271727uy.FRMC.integration.manors_bounty;

import java.util.Map;
import net.mcreator.manors_bounty.init.ManorsBountyModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public final class NestSearchHelper {
    private static final int HORIZONTAL_RANGE = 16;
    private static final int VERTICAL_RANGE = 4;

    private NestSearchHelper() {
    }

    public static BlockPos findNearestAvailableNest(Mob mob) {
        Level level = mob.level();
        if (!(level instanceof ServerLevel)) {
            return null;
        }

        BlockPos origin = mob.blockPosition();
        int minChunkX = (origin.getX() - HORIZONTAL_RANGE) >> 4;
        int maxChunkX = (origin.getX() + HORIZONTAL_RANGE) >> 4;
        int minChunkZ = (origin.getZ() - HORIZONTAL_RANGE) >> 4;
        int maxChunkZ = (origin.getZ() + HORIZONTAL_RANGE) >> 4;
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    int dx = pos.getX() - origin.getX();
                    int dy = pos.getY() - origin.getY();
                    int dz = pos.getZ() - origin.getZ();
                    if (Math.abs(dx) > HORIZONTAL_RANGE || Math.abs(dy) > VERTICAL_RANGE || Math.abs(dz) > HORIZONTAL_RANGE) {
                        continue;
                    }
                    if (dx * dx + dz * dz > nearestDistance || !level.getBlockState(pos).is(ManorsBountyModBlocks.HAY_NEST.get())) {
                        continue;
                    }
                    if (!hasEmptyEggSlot(entry.getValue())) {
                        continue;
                    }

                    double distance = mob.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = pos.immutable();
                    }
                }
            }
        }

        return nearest;
    }

    private static boolean hasEmptyEggSlot(BlockEntity blockEntity) {
        return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).map(handler -> {
            int slotCount = Math.min(3, handler.getSlots());
            for (int slot = 0; slot < slotCount; slot++) {
                if (handler.getStackInSlot(slot).isEmpty()) {
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }
}
