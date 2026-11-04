package com.y271727uy.FRMC.integration.untamedwilds;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import untamedwilds.entity.ComplexMob;

/** Prevents Untamed Wilds mobs from damaging one another during benchmark setups. */
public final class UntamedWildsFriendlyFireHandler {
    private UntamedWildsFriendlyFireHandler() {
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ComplexMob)
                || !(event.getSource().getEntity() instanceof ComplexMob)) {
            return;
        }

        event.setCanceled(true);
    }
}
