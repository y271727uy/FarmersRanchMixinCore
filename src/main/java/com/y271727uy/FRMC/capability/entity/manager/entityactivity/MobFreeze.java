package com.y271727uy.FRMC.capability.entity.manager.entityactivity;

import net.minecraft.world.entity.Mob;

/** Shared helpers for every AI freeze that works by skipping Mob.serverAiStep. */
public final class MobFreeze {
    private MobFreeze() {
    }

    /**
     * LivingEntity.aiStep still calls travel() with the last movement inputs after serverAiStep is skipped, so a
     * mob frozen mid-step keeps walking straight ahead until it hits something. Clear those inputs so it stands
     * still; the next unfrozen serverAiStep sets them again from its move control.
     */
    public static void haltInputs(Mob mob) {
        mob.setSpeed(0.0F);
        mob.setXxa(0.0F);
        mob.setYya(0.0F);
        mob.setZza(0.0F);
        mob.setJumping(false);
    }
}
