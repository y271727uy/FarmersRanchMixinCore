package com.y271727uy.FRMC.integration.citadel;

import com.github.alexthe666.citadel.Citadel;
import com.github.alexthe666.citadel.ServerProxy;
import com.y271727uy.FRMC.FRMCMod;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Citadel 把服务器存在一个静态字段里，只在下一个服务器启动时才覆盖。
 * 退出单人存档后，这个引用会让整个已关闭的世界留在内存里直到再次进入存档。
 */
@Mod.EventBusSubscriber(modid = FRMCMod.MODID)
public final class CitadelServerRelease {
    private CitadelServerRelease() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerStopped(ServerStoppedEvent event) {
        if (ModList.get().isLoaded("citadel")) {
            CitadelAccess.release(event);
        }
    }

    /** 单独成类，未安装 Citadel 时不会链接它的类。 */
    private static final class CitadelAccess {
        private static void release(ServerStoppedEvent event) {
            ServerProxy proxy = Citadel.PROXY;
            if (proxy != null && proxy.getMinecraftServer() == event.getServer()) {
                ServerProxy.setMinecraftServer(null);
            }
        }
    }
}
