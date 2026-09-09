package com.y271727uy.FRMC.client.jade;

import snownee.jade.api.Identifiers;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * 只移除 Farm &amp; Charm 食槽在 Jade 中显示的通用物品存储行。
 *
 * <p>方块名称、模组名称以及其他正常的 Jade 方块信息都会保留。之所以
 * 隐藏物品行，是因为 Farm &amp; Charm 的动物取食目标会直接减少 {@code SIZE}，
 * 可能导致方块实体中保存的 {@code ItemStack} 暂时过期。</p>
 */
@WailaPlugin("frmc")
public final class FRMCJadePlugin implements IWailaPlugin {
    private static final String FEEDING_TROUGH_BLOCK_CLASS =
        "net.satisfy.farm_and_charm.core.block.FeedingTroughBlock";

    /** 此兼容修复不需要注册服务端 Jade 提供器。 */
    @Override
    public void register(IWailaCommonRegistration registration) {
    }

    /** 仅对食槽过滤 Jade 的通用物品存储组件。 */
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addTooltipCollectedCallback(0, (tooltip, accessor) -> {
            if (accessor instanceof snownee.jade.api.BlockAccessor blockAccessor
                    && blockAccessor.getBlock().getClass().getName().equals(FEEDING_TROUGH_BLOCK_CLASS)) {
                tooltip.remove(Identifiers.UNIVERSAL_ITEM_STORAGE);
            }
        });
    }
}
