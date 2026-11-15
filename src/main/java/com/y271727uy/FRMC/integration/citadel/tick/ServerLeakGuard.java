package com.y271727uy.FRMC.integration.citadel.tick;

import com.github.alexthe666.citadel.server.tick.ServerTickRateTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 清理 Citadel TickRateTracker 中残留的已卸载实体/维度引用（防内存泄漏）。仅在安装 Citadel 时注册。 */
public final class ServerLeakGuard {
    public static final ServerLeakGuard INSTANCE = new ServerLeakGuard();

    private ServerLeakGuard() {}

    @SubscribeEvent
    public void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ServerTickRateTracker tracker = CitadelDataCache.existingTracker(level.getServer());
        if (tracker instanceof TickListAccess access && !tracker.specialTickRateEntities.isEmpty()) {
            access.frmc$purgeEntity(event.getEntity());
        }
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (CitadelDataCache.existingTracker(level.getServer()) instanceof TickListAccess access) {
            access.frmc$purgeLevel(level);
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        if (CitadelDataCache.existingTracker(event.getServer()) instanceof TickListAccess access) {
            access.frmc$purgeAll();
        }
        CitadelDataCache.invalidate();
    }
}
