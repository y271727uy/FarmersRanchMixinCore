package com.y271727uy.FRMC.capability.data_group.matcher;

import com.y271727uy.FRMC.capability.data_group.provider.DataProvider;
import com.y271727uy.FRMC.capability.data_group.provider.DataProviderRegistry;
import com.y271727uy.FRMC.capability.data_group.target.BlockTarget;
import com.y271727uy.FRMC.capability.data_group.target.MatchTarget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Matches blocks using a data provider to extract field values.
 * Used for blocks that store species/variant data in BlockEntity.
 */
public final class BlockProviderMatcher implements GroupMatcher {
    private final ResourceLocation blockId;
    private final String providerId;
    private final String fieldName;
    @Nullable
    private final String expectedValue;

    public BlockProviderMatcher(ResourceLocation blockId, String providerId, String fieldName) {
        this(blockId, providerId, fieldName, null);
    }

    public BlockProviderMatcher(ResourceLocation blockId, String providerId, String fieldName, @Nullable String expectedValue) {
        this.blockId = blockId;
        this.providerId = providerId;
        this.fieldName = fieldName;
        this.expectedValue = expectedValue;
    }

    @Override
    public boolean matches(MatchTarget target) {
        if (!target.isBlock()) {
            return false;
        }

        BlockTarget blockTarget = target.asBlockTarget();
        BlockState state = blockTarget.getState();
        
        // Check block ID first
        ResourceLocation stateId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (!blockId.equals(stateId)) {
            return false;
        }

        // Get provider
        DataProvider provider = DataProviderRegistry.get(providerId);
        if (provider == null || !provider.supportsBlock(blockTarget)) {
            return false;
        }

        // Get field value
        String value = provider.getBlockField(blockTarget, fieldName);
        if (value == null) {
            return false;
        }

        // If no expected value, just check existence
        if (expectedValue == null) {
            return true;
        }

        // Check value match
        return expectedValue.equals(value);
    }

    @Override
    public String getType() {
        return "block_provider";
    }

    public ResourceLocation getBlockId() {
        return blockId;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Nullable
    public String getExpectedValue() {
        return expectedValue;
    }
}
