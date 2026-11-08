package com.y271727uy.FRMC.integration.manors_bounty;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.mcreator.manors_bounty.init.ManorsBountyModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public final class NestSearchHelper {
    private static final int HORIZONTAL_RANGE = 16;
    private static final int VERTICAL_RANGE = 4;

    private NestSearchHelper() {
    }

    public static int failedSearchCooldown(Mob mob) {
        return 8 + searchStagger(mob);
    }

    public static int searchStagger(Mob mob) {
        // Spread nest searches from flocks that become ready to lay together.
        return mob.getId() & 7;
    }

    public static Set<BlockPos> findAvailableNests(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return Set.of();
        }

        BlockPos origin = mob.blockPosition();
        int minChunkX = (origin.getX() - HORIZONTAL_RANGE) >> 4;
        int maxChunkX = (origin.getX() + HORIZONTAL_RANGE) >> 4;
        int minChunkZ = (origin.getZ() - HORIZONTAL_RANGE) >> 4;
        int maxChunkZ = (origin.getZ() + HORIZONTAL_RANGE) >> 4;
        Set<BlockPos> nests = new HashSet<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    int dx = pos.getX() - origin.getX();
                    int dy = pos.getY() - origin.getY();
                    int dz = pos.getZ() - origin.getZ();
                    if (Math.abs(dx) > HORIZONTAL_RANGE || Math.abs(dy) > VERTICAL_RANGE || Math.abs(dz) > HORIZONTAL_RANGE) {
                        continue;
                    }
                    if (isAvailableNest(chunk, pos, entry.getValue())) {
                        nests.add(pos.immutable());
                    }
                }
            }
        }

        return nests;
    }

    public static boolean isAvailableNest(Mob mob, BlockPos pos) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && isAvailableNest(chunk, pos, chunk.getBlockEntity(pos));
    }

    private static boolean isAvailableNest(LevelChunk chunk, BlockPos pos, BlockEntity blockEntity) {
        return blockEntity != null && !blockEntity.isRemoved()
                && chunk.getBlockState(pos).is(ManorsBountyModBlocks.HAY_NEST.get())
                && hasEmptyEggSlot(blockEntity);
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
