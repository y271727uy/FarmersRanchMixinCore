package com.y271727uy.FRMC.integration.embeddium.perf;

import me.jellysquid.mods.sodium.client.model.color.DefaultColorProviders;

/**
 * 判断一个颜色提供器的结果能否在同一个方块模型内按染色索引复用。
 * 只认 Embeddium 包装原版 BlockColor 的适配器：它对四个顶点填同一个值，且只取决于方块状态、位置和染色索引。
 * 生物群系混合的提供器（草、树叶、水）逐顶点取色，其他 mod 通过 API 注册的提供器行为未知，都不缓存。
 * 思路来自 Emberbium（Embeddium 的 fork，LGPL-3.0），fork 里是“非混合的都缓存”，这里收窄了范围。
 */
public final class BlockTintCache {
    private static final Class<?> VANILLA_ADAPTER = findVanillaAdapter();

    private BlockTintCache() {
    }

    public static boolean isCacheable(Object colorProvider) {
        return VANILLA_ADAPTER != null && colorProvider.getClass() == VANILLA_ADAPTER;
    }

    private static Class<?> findVanillaAdapter() {
        try {
            return Class.forName(
                DefaultColorProviders.class.getName() + "$VanillaAdapter",
                false,
                DefaultColorProviders.class.getClassLoader());
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }
}
