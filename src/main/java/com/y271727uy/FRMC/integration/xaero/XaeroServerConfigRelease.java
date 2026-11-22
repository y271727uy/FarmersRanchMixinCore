package com.y271727uy.FRMC.integration.xaero;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * Xaero 配置库每个配置通道的 {@code ServerConfigManager} 都持有 {@code MinecraftServer}，
 * 只有下一次服务器启动或有人调用 {@code getServer()} 时才会丢弃，退出单人存档后会把关闭的世界留在内存里。
 * 服务器停止后主动置空。
 */
public final class XaeroServerConfigRelease {
    private static final Logger LOGGER = LogUtils.getLogger();

    private XaeroServerConfigRelease() {
    }

    public static void register() {
        ModList mods = ModList.get();
        if (mods.isLoaded("xaerolib") || mods.isLoaded("xaerominimap") || mods.isLoaded("xaeroworldmap")) {
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, XaeroServerConfigRelease::onServerStopped);
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        try {
            XaeroLibAccess.release();
        } catch (RuntimeException | LinkageError e) {
            LOGGER.debug("[FRMC] Could not release Xaero lib server references", e);
        }
    }

    /** 单独成类，未安装 Xaero 时不会链接它的类。 */
    private static final class XaeroLibAccess {
        private static void release() {
            for (var channel : xaero.lib.common.config.channel.register.ConfigChannelRegistry.INSTANCE) {
                var manager = channel.getServerConfigManager();
                if (manager != null) {
                    // 服务器已停止，isRunning() 为 false，不会触发"多服务器"检查。
                    manager.setServer(null);
                }
            }
        }
    }
}
