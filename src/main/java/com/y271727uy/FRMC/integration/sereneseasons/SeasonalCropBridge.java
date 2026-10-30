package com.y271727uy.FRMC.integration.sereneseasons;

import com.y271727uy.FRMC.capability.data_group.DataGroupRegistry;
import com.y271727uy.FRMC.capability.data_group.target.BlockTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

import java.util.Set;

/**
 * Bridge between FRMC data groups and Serene Seasons crop growth.
 * Translates data group membership into seasonal fertility rules.
 */
public final class SeasonalCropBridge {
    private SeasonalCropBridge() {
    }

    /**
     * Checks if a block at the given position is allowed to grow in the current season.
     * This checks all registered data groups and their seasonal mappings.
     * 
     * @return true if growth is allowed, false if blocked by season
     */
    public static boolean isGrowthAllowed(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return true;
        }

        // Get current season
        Season.SubSeason currentSeason = SeasonHelper.getSeasonState(serverLevel).getSubSeason();

        // Create block target
        BlockTarget target = BlockTarget.of(serverLevel, pos, state);

        // Get all groups this block belongs to
        Set<ResourceLocation> matchingGroups = DataGroupRegistry.getMatchingGroups(target);

        if (matchingGroups.isEmpty()) {
            // Not in any data group, allow growth (vanilla behavior)
            return true;
        }

        // Check if any matching group allows growth in current season
        for (ResourceLocation groupId : matchingGroups) {
            if (SeasonalGroupMapping.isAllowedInSeason(groupId, currentSeason)) {
                return true;
            }
        }

        // Block is in at least one group, but none allow current season
        return false;
    }

    /**
     * Returns the growth rate modifier based on seasonal group membership.
     * Can be used to slow down growth instead of blocking it entirely.
     * 
     * @return multiplier (1.0 = normal, 0.0 = blocked, >1.0 = faster)
     */
    public static float getGrowthRateModifier(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 1.0f;
        }

        Season.SubSeason currentSeason = SeasonHelper.getSeasonState(serverLevel).getSubSeason();
        BlockTarget target = BlockTarget.of(serverLevel, pos, state);
        Set<ResourceLocation> matchingGroups = DataGroupRegistry.getMatchingGroups(target);

        if (matchingGroups.isEmpty()) {
            return 1.0f;
        }

        // Check if growth is allowed
        boolean allowed = false;
        for (ResourceLocation groupId : matchingGroups) {
            if (SeasonalGroupMapping.isAllowedInSeason(groupId, currentSeason)) {
                allowed = true;
                break;
            }
        }

        return allowed ? 1.0f : 0.0f;
    }
}
