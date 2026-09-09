package com.y271727uy.FRMC.integration.farm_and_charm;

import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.satisfy.farm_and_charm.core.block.FeedingTroughBlock;
import net.satisfy.farm_and_charm.core.registry.TagRegistry;

/**
 * 为 Farm &amp; Charm 食槽补充玩家手动放入饲料的交互逻辑。
 *
 * <p>Farm &amp; Charm 原版的 {@code use} 方法只接受
 * {@code minecraft:villager_plantable_seeds}，并且只修改方块的
 * {@code SIZE} 属性，不会同步更新方块实体。本处理器保持 fodder 标签的
 * 独立语义，并使用食槽的容器接口写入物品，与漏斗使用相同的数据路径。</p>
 *
 * <p>这里使用交互事件，而不是直接 Mixin 到 {@code FeedingTroughBlock}，
 * 从而避免把兼容逻辑加入目标类的转换链，减少与其他类转换器发生冲突的风险。</p>
 * 改用此方法实现是因为以下原因
 * 1.避免污染minecraft:villager_plantable_seeds标签的语义
 * 2.因为直接使用 @Overwrite 会导致 SDM 的查找字体而崩溃，但是很明显我不想硬吃 SDM 这坨大粪
 *
 * 之前的实现方式是直接 Mixin 到 net.satisfy.farm_and_charm.core.block.FeedingTroughBlock 并注入它的 use(...)
 * 当原方法返回 PASS 时，额外检查：stack.is(TagRegistry.FEEDING_TROUGH_FODDER)然后修改 SIZE、减少玩家物品
 * 但是关键是他妈的 SDM 的字体异常是另一条独立错误链，理论来说不应该和之前的 Mixin 有关联,但是二分法证明的确如此
 * 我的评价是：傻逼SDM
 */
public final class FeedingTroughInteractionHandler {
    private FeedingTroughInteractionHandler() {
    }

    /**
     * 向食槽中放入一个带有 fodder 标签的物品，并从玩家手中消耗一个物品。
     * 客户端返回相同的 sided 结果，避免原版方块交互再次执行。
     *
     * @param event Forge 的右键方块交互事件
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }

        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof FeedingTroughBlock)) {
            return;
        }

        ItemStack held = event.getEntity().getItemInHand(event.getHand());
        if (!held.is(TagRegistry.FEEDING_TROUGH_FODDER) || state.getValue(FeedingTroughBlock.SIZE) >= 4) {
            return;
        }

        if (!event.getLevel().isClientSide) {
            BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
            if (!(blockEntity instanceof Container container)) {
                return;
            }

            // Farm & Charm 的动物目标会直接减少 SIZE，但保留方块实体中的旧数量。
            // 在执行标准容器插入流程前，先修正这个过期数量。
            int stateSize = state.getValue(FeedingTroughBlock.SIZE);
            ItemStack stored = container.getItem(0);
            if (stored.getCount() != stateSize) {
                container.setItem(0, stateSize == 0 ? ItemStack.EMPTY : stored.copyWithCount(stateSize));
            }

            if (!container.canPlaceItem(0, held)) {
                return;
            }

            container.setItem(0, held.copyWithCount(1));
            if (!event.getEntity().getAbilities().instabuild) {
                held.shrink(1);
            }
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }
}

/**
 * SDM 那份日志的异常是
 * java.util.NoSuchElementException: No value present
 * at net.sixik.sdm_core.utils.FileUtils.readResourceBytes(FileUtils.java:16)
 * at net.sixik.sdm_core.modules.imgui.init.ImGuiHandler.initFonts(ImGuiHandler.java:127)
 * 发生在 SDM Core 初始化 ImGui 字体时，核心问题是资源读取返回了空的 Optional
 *
 * 而FRMC这边发生在 Mixin 准备阶段，甚至还没进入正常游戏初始化，也没有经过 SDM 的字体初始化代码
 * 那么在理论上来说日志里面的是两条独立的异常链，根本无法从这些日志证明 SDM 导致了 FRMC 的 Mixin 崩溃或者是 FRMC 导致了 SDM崩溃
 * 但是何意味呢？？？
 * 事实上啊，FRMC 往 FeedingTroughBlock 注入的 Mixin 的的确确会导致 SDM 崩溃，就很离谱
 */