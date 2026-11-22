package com.y271727uy.FRMC.integration.kubejs;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * KubeJS 把上一次数据包加载的 {@code ReloadableServerResources}（配方、标签、战利品表以及整个注册表视图）
 * 存在静态字段 {@code KubeJSReloadListener.resources} 里，直到下一次服务器加载数据包才覆盖，
 * 退出单人存档后会把关闭的世界留在内存里。它的读取方都先判空，下次加载时会重新赋值。
 */
public final class KubeJSServerResourcesRelease {
    private static final Logger LOGGER = LogUtils.getLogger();

    private KubeJSServerResourcesRelease() {
    }

    public static void register() {
        if (ModList.get().isLoaded("kubejs")) {
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, KubeJSServerResourcesRelease::onServerStopped);
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        try {
            KubeJSAccess.release();
        } catch (RuntimeException | LinkageError e) {
            LOGGER.debug("[FRMC] Could not release KubeJS server resources", e);
        }
    }

    /** 单独成类，未安装 KubeJS 时不会链接它的类。 */
    private static final class KubeJSAccess {
        private static void release() {
            dev.latvian.mods.kubejs.server.KubeJSReloadListener.resources = null;
        }
    }
}
