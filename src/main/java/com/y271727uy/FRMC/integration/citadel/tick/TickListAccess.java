package com.y271727uy.FRMC.integration.citadel.tick;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** 由 TickRateTrackerMixin 实现，用于清理 Citadel 持有的已卸载实体引用。 */
public interface TickListAccess {
    void frmc$purgeEntity(Entity entity);

    void frmc$purgeLevel(Level level);

    void frmc$purgeAll();
}
