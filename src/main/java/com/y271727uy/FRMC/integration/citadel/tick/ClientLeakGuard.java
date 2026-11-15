package com.y271727uy.FRMC.integration.citadel.tick;

import com.github.alexthe666.citadel.client.tick.ClientTickRateTracker;
import com.y271727uy.FRMC.FRMCMod;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** 客户端侧：清理 ClientTickRateTracker 中的残留实体，并在登出时清空悬停动画状态。 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = FRMCMod.MODID)
public final class ClientLeakGuard {
    private ClientLeakGuard() {}

    private static ClientTickRateTracker tracker() {
        // Citadel 未安装时事件仍会到达这里，直接跳过（也避免加载 Citadel 类）。
        return ModList.get().isLoaded("citadel") ? ClientTrackerCache.existing() : null;
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ClientLevel)) {
            return;
        }
        ClientTickRateTracker tracker = tracker();
        if (tracker instanceof TickListAccess access && !tracker.specialTickRateEntities.isEmpty()) {
            access.frmc$purgeEntity(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel level && tracker() instanceof TickListAccess access) {
            access.frmc$purgeLevel(level);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (!ModList.get().isLoaded("citadel")) {
            return;
        }
        if (tracker() instanceof TickListAccess access) {
            access.frmc$purgeAll();
        }
        HoverAnimationState.clearAll();
    }
}
