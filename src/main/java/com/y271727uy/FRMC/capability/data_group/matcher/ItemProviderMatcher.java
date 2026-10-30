package com.y271727uy.FRMC.capability.data_group.matcher;

import com.y271727uy.FRMC.capability.data_group.provider.DataProvider;
import com.y271727uy.FRMC.capability.data_group.provider.DataProviderRegistry;
import com.y271727uy.FRMC.capability.data_group.target.MatchTarget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Matches items using a data provider to extract field values.
 * Used for items with complex data structures (e.g., Forestry genetics).
 */
public final class ItemProviderMatcher implements GroupMatcher {
    private final ResourceLocation itemId;
    private final String providerId;
    private final String fieldName;
    @Nullable
    private final String expectedValue;

    public ItemProviderMatcher(ResourceLocation itemId, String providerId, String fieldName) {
        this(itemId, providerId, fieldName, null);
    }

    public ItemProviderMatcher(ResourceLocation itemId, String providerId, String fieldName, @Nullable String expectedValue) {
        this.itemId = itemId;
        this.providerId = providerId;
        this.fieldName = fieldName;
        this.expectedValue = expectedValue;
    }

    @Override
    public boolean matches(MatchTarget target) {
        if (!target.isItem()) {
            return false;
        }

        ItemStack stack = target.asItemStack();
        if (stack.isEmpty()) {
            return false;
        }

        // Check item ID first
        ResourceLocation stackId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (!itemId.equals(stackId)) {
            return false;
        }

        // Get provider
        DataProvider provider = DataProviderRegistry.get(providerId);
        if (provider == null || !provider.supportsItem(stack)) {
            return false;
        }

        // Get field value
        String value = provider.getItemField(stack, fieldName);
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
        return "item_provider";
    }

    public ResourceLocation getItemId() {
        return itemId;
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
