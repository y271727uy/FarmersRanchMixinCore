package com.y271727uy.FRMC.integration.alexsmobs;

import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.mixin.alexsmobs.AMServerEventsAccessor;
import com.y271727uy.FRMC.mixin.alexsmobs.AMWorldDataAccessor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Alex's Mobs 把 {@code AMWorldData} 和搁浅抹香鲸生成器按 {@code Level} 存进静态 HashMap，从不移除。
 * 退出单人存档后，这两个 map 会把已关闭的整个世界一直留在内存里，直到下次进入同一个维度才被覆盖。
 *
 * <p>只在服务器完全停止（存档已写完）之后清空：此时这些世界不可能再被访问，
 * 下次启动时 Alex's Mobs 会照常从存档里按需重新加载。
 */
public final class AlexsMobsLevelMapRelease {
    private static final Logger LOGGER = LogUtils.getLogger();

    private AlexsMobsLevelMapRelease() {
    }

    public static void register() {
        if (ModList.get().isLoaded("alexsmobs")) {
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, AlexsMobsLevelMapRelease::onServerStopped);
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        clear("AMWorldData.dataMap", AMWorldDataAccessor::frmc$dataMap);
        clear("ServerEvents.BEACHED_CACHALOT_WHALE_SPAWNER_MAP", AMServerEventsAccessor::frmc$beachedWhaleSpawners);
    }

    private static void clear(String name, java.util.function.Supplier<? extends Map<?, ?>> source) {
        try {
            Map<?, ?> map = source.get();
            if (map != null) {
                map.clear();
            }
        } catch (RuntimeException | LinkageError e) {
            // mixin 被黑名单禁用或 Alex's Mobs 改了字段时，只是少清一处缓存。
            LOGGER.debug("[FRMC] Could not release Alex's Mobs {}", name, e);
        }
    }
}
