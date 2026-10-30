package com.y271727uy.FRMC.mixin.sereneseasons;

import com.y271727uy.FRMC.integration.sereneseasons.SeasonalTooltipHelper;
import glitchcore.event.client.ItemTooltipEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects FRMC data group support into Serene Seasons' tooltip system.
 * Allows Forestry saplings and other data-group items to show seasonal icons.
 */
@Pseudo
@Mixin(targets = "sereneseasons.init.ModFertility", remap = false)
public abstract class ModFertilityTooltipMixin {

    /**
     * Injects before Serene Seasons checks its tags.
     * If item is in a FRMC data group, add it to Serene Seasons' internal map.
     */
    @Inject(
        method = "setupTooltips(Lglitchcore/event/client/ItemTooltipEvent;)V",
        at = @At("HEAD"),
        remap = false,
        require = 0
    )
    private static void frmc$injectDataGroupTooltips(ItemTooltipEvent event, CallbackInfo ci) {
        ItemStack stack = event.getStack();
        
        // Check if this item is managed by FRMC data groups
        if (!SeasonalTooltipHelper.isManagedByDataGroup(stack)) {
            return;
        }

        // Get season bitmask from data groups
        int seasonBitmask = SeasonalTooltipHelper.getSeasonBitmask(stack);
        
        if (seasonBitmask == 0) {
            // No seasons configured, don't show tooltip
            return;
        }

        // Serene Seasons indexes its native seed tooltip map by item ID only.
        // Forestry stores the species in ItemStack data, so inserting
        // forestry:sapling into that map would make every Forestry sapling
        // display the same seasons. Render the translated tooltip directly
        // for this exact ItemStack instead.
        addSeasonTooltip(event, seasonBitmask);
    }

    private static void addSeasonTooltip(ItemTooltipEvent event, int seasonBitmask) {
        if ((seasonBitmask & 15) == 15) {
            event.getTooltip().add(
                Component.translatable("desc.sereneseasons.fertile_seasons")
                    .append(":")
            );
            event.getTooltip().add(
                Component.translatable("desc.sereneseasons.year_round")
                    .withStyle(ChatFormatting.LIGHT_PURPLE)
            );
            return;
        }

        event.getTooltip().add(
            Component.translatable("desc.sereneseasons.fertile_seasons")
                .append(":")
        );
        if ((seasonBitmask & 1) != 0) {
            event.getTooltip().add(
                Component.translatable("desc.sereneseasons.spring")
                    .withStyle(ChatFormatting.GREEN)
            );
        }
        if ((seasonBitmask & 2) != 0) {
            event.getTooltip().add(
                Component.translatable("desc.sereneseasons.summer")
                    .withStyle(ChatFormatting.YELLOW)
            );
        }
        if ((seasonBitmask & 4) != 0) {
            event.getTooltip().add(
                Component.translatable("desc.sereneseasons.autumn")
                    .withStyle(ChatFormatting.GOLD)
            );
        }
        if ((seasonBitmask & 8) != 0) {
            event.getTooltip().add(
                Component.translatable("desc.sereneseasons.winter")
                    .withStyle(ChatFormatting.AQUA)
            );
        }
    }
}
