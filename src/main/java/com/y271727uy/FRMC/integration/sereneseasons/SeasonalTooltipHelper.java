package com.y271727uy.FRMC.integration.sereneseasons;

import com.y271727uy.FRMC.capability.data_group.DataGroupRegistry;
import com.y271727uy.FRMC.capability.data_group.target.ItemTarget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import sereneseasons.api.season.Season;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Helper to expose FRMC data groups to Serene Seasons tooltip system.
 * Allows Serene Seasons to display seasonal icons on Forestry saplings and other data-group items.
 */
public final class SeasonalTooltipHelper {
    
    private SeasonalTooltipHelper() {
    }

    /**
     * Gets the seasons when this item can grow, based on its data group membership.
     * Returns a bitmask compatible with Serene Seasons' internal format.
     * 
     * @return bitmask: 1=spring, 2=summer, 4=autumn, 8=winter, 0=none, 15=all
     */
    public static int getSeasonBitmask(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        ItemTarget target = ItemTarget.of(stack);
        Set<ResourceLocation> groups = DataGroupRegistry.getMatchingGroups(target);
        
        if (groups.isEmpty()) {
            // Not in any data group, return 0 to let Serene Seasons handle it normally
            return 0;
        }

        // Check all 12 sub-seasons and collect unique main seasons
        boolean hasSpring = false;
        boolean hasSummer = false;
        boolean hasAutumn = false;
        boolean hasWinter = false;

        for (ResourceLocation groupId : groups) {
            // Check each sub-season
            if (SeasonalGroupMapping.isAllowedInSeason(groupId, Season.SubSeason.EARLY_SPRING) ||
                SeasonalGroupMapping.isAllowedInSeason(groupId, Season.SubSeason.MID_SPRING) ||
                SeasonalGroupMapping.isAllowedInSeason(groupId, Season.SubSeason.LATE_SPRING)) {
                hasSpring = true;
            }
            
            if (SeasonalGroupMapping.isAllowedInSeason(groupId, Season.SubSeason.EARLY_SUMMER) ||
                SeasonalGroupMapping.isAllowedInSeason(groupId, Season.SubSeason.MID_SUMMER) ||
                SeasonalGroupMapping.isAllowedInSeason(groupId, Season.SubSeason.LATE_SUMMER)) {
                hasSummer = true;
            }
            
            if (SeasonalGroupMapping.isAllowedInSeason(groupId, Season.SubSeason.EARLY_AUTUMN) ||
                SeasonalGroupMapping.isAllowedInSeason(groupId, Season.SubSeason.MID_AUTUMN) ||
                SeasonalGroupMapping.isAllowedInSeason(groupId, Season.SubSeason.LATE_AUTUMN)) {
                hasAutumn = true;
            }
            
            if (SeasonalGroupMapping.isAllowedInSeason(groupId, Season.SubSeason.EARLY_WINTER) ||
                SeasonalGroupMapping.isAllowedInSeason(groupId, Season.SubSeason.MID_WINTER) ||
                SeasonalGroupMapping.isAllowedInSeason(groupId, Season.SubSeason.LATE_WINTER)) {
                hasWinter = true;
            }
        }

        // Convert to bitmask (Serene Seasons format)
        int bitmask = 0;
        if (hasSpring) bitmask |= 1;  // 0001
        if (hasSummer) bitmask |= 2;  // 0010
        if (hasAutumn) bitmask |= 4;  // 0100
        if (hasWinter) bitmask |= 8;  // 1000
        
        return bitmask;
    }

    /**
     * Gets a list of season names for display.
     * Used by custom tooltip rendering if needed.
     */
    public static List<Season> getAllowedSeasons(ItemStack stack) {
        int bitmask = getSeasonBitmask(stack);
        List<Season> seasons = new ArrayList<>();
        
        if ((bitmask & 1) != 0) seasons.add(Season.SPRING);
        if ((bitmask & 2) != 0) seasons.add(Season.SUMMER);
        if ((bitmask & 4) != 0) seasons.add(Season.AUTUMN);
        if ((bitmask & 8) != 0) seasons.add(Season.WINTER);
        
        return seasons;
    }

    /**
     * Checks if this item belongs to any FRMC data group.
     * If true, FRMC should handle its seasonal tooltip.
     */
    public static boolean isManagedByDataGroup(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        ItemTarget target = ItemTarget.of(stack);
        Set<ResourceLocation> groups = DataGroupRegistry.getMatchingGroups(target);
        return !groups.isEmpty();
    }
}
