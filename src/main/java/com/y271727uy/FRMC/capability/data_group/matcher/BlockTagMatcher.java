package com.y271727uy.FRMC.capability.data_group.matcher;

import com.y271727uy.FRMC.capability.data_group.target.BlockTarget;
import com.y271727uy.FRMC.capability.data_group.target.MatchTarget;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Matches blocks by tag.
 */
public final class BlockTagMatcher implements GroupMatcher {
    private final TagKey<Block> tag;

    public BlockTagMatcher(TagKey<Block> tag) {
        this.tag = tag;
    }

    @Override
    public boolean matches(MatchTarget target) {
        if (!target.isBlock()) {
            return false;
        }
        BlockTarget blockTarget = target.asBlockTarget();
        BlockState state = blockTarget.getState();
        return state.is(tag);
    }

    @Override
    public String getType() {
        return "block_tag";
    }

    public TagKey<Block> getTag() {
        return tag;
    }
}
