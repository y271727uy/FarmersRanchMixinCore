package com.y271727uy.FRMC.integration.nethers_exoticism;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class NethersExoticismAdvancements {
    private NethersExoticismAdvancements() {
    }

    public static void award(ServerPlayer player, ResourceLocation id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement == null || player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            return;
        }
        for (String criterion : player.getAdvancements().getOrStartProgress(advancement).getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
