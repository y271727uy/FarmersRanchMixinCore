package com.y271727uy.FRMC.mixin.minecraft.villager.optimization;

import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only view of the post-trade level-up countdown, which only advances while the villager's AI runs. */
@Mixin(Villager.class)
public interface VillagerMerchantAccessor {
    @Accessor("updateMerchantTimer")
    int frmc$updateMerchantTimer();
}
