package com.y271727uy.FRMC.integration.delighto_flight;

import net.minecraft.world.entity.ai.memory.WalkTarget;

/** Identifies work destinations created by Delight o' Flight. */
public final class ChefWalkTarget extends WalkTarget {
    public ChefWalkTarget(WalkTarget original) {
        super(original.getTarget(), original.getSpeedModifier(), original.getCloseEnoughDist());
    }
}
