package com.y271727uy.FRMC.integration.embeddium.perf;

import com.y271727uy.FRMC.integration.embeddium.EmbeddiumPortConfig;
import me.jellysquid.mods.sodium.client.SodiumClientMod;

/**
 * 把“标记可见区块段里的动画贴图为活跃”从每帧一次改为每 tick 一次。
 * 贴图动画只在客户端 tick 时推进，活跃标记也在那时被消费并清除，所以帧率高于 20 时每帧标记是重复劳动。
 * 移植自 Emberbium（Embeddium 的 fork，LGPL-3.0）。由 SodiumWorldRendererSpriteTickMixin 实现。
 */
public interface VisibleSpriteTicker {
    void frmc$markVisibleSprites();

    /** 只有 Embeddium 的“仅为可见纹理播放动画”开着时才接管；关着时标记本来就不起作用，保持原样。 */
    static boolean perTick() {
        return EmbeddiumPortConfig.spriteMarkingPerTick()
            && SodiumClientMod.options().performance.animateOnlyVisibleTextures;
    }
}
