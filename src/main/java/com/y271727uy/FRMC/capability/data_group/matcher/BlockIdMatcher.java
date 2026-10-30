package com.y271727uy.FRMC.capability.data_group.matcher;

import com.y271727uy.FRMC.capability.data_group.target.BlockTarget;
import com.y271727uy.FRMC.capability.data_group.target.MatchTarget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Matches blocks by their registry ID.
 */
public final class BlockIdMatcher implements GroupMatcher {
    private final ResourceLocation blockId;

    public BlockIdMatcher(ResourceLocation blockId) {
        this.blockId = blockId;
    }

    @Override
    public boolean matches(MatchTarget target) {
        if (!target.isBlock()) {
            return false;
        }
        BlockTarget blockTarget = target.asBlockTarget();
        BlockState state = blockTarget.getState();
        ResourceLocation stateId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return blockId.equals(stateId);
    }

    @Override
    public String getType() {
        return "block_id";
    }

    public ResourceLocation getBlockId() {
        return blockId;
    }
}
