package com.y271727uy.FRMC.mixin.thirst;

import dev.ghen.thirst.content.purity.WaterPurity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 2026/8/20 凌晨1：15
 * 问题出现时间：2026/8/19 晚上21:13
 * 基本现象：熔炉烧水但是游戏崩溃，疑似会造成坏档，但是从报错日志分析不会。日志分析出100,70,-1731的熔炉导致tick错误
 * 临时解决方案：口渴 多态合成 临时关闭下，进入存档后破坏熔炉
 *
 * 最初看到崩溃日志时，第一反应是世界或存档坏了，因为报错发生在单人服务端线程，而且位置稳定地落在一个熔炉 tick 上，坐标是 (100,70,-1731)。不过继续查看 crash report 后，可以确认这不是整档损坏，而是一次典型的 Ticking block entity 异常，也就是说，世界本身还能读，只是某个方块实体在运行过程中触发了崩溃。
 * Description: Ticking block entity
 * java.lang.NullPointerException: Cannot invoke "net.minecraft.nbt.CompoundTag.m_128441_(String)" because the return value of "net.minecraft.world.item.ItemStack.m_41783_()" is null
 *     at net.minecraft.world.item.crafting.AbstractCookingRecipe.modify$bgi000$thirst$matches(AbstractCookingRecipe.java:516)
 *     at net.minecraft.world.item.crafting.AbstractCookingRecipe.m_58
 *     18_(AbstractCookingRecipe.java:34)
 *     at com.illusivesoulworks.polymorph.common.capability.AbstractRecipeData.lambda$getRecipe$1(AbstractRecipeData.java:77)
 *     at com.illusivesoulworks.polymorph.common.crafting.RecipeSelection.getBlockEntityRecipe(RecipeSelection.java:91)
 *     at com.y271727uy.FRMC.compat.polymorph.PolymorphIntegration.getBlockEntityRecipe(PolymorphIntegration.java:28)
 *     at com.y271727uy.FRMC.recipe.manager.RecipeManagement.m_220248_(RecipeManagement.java:95)
 *     at net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.m_155013_(AbstractFurnaceBlockEntity.java:249)
 * 从这段栈里，最直接的异常点其实已经很清楚：Thirst was Taken 在 AbstractCookingRecipe.matches() 里，对 ItemStack.getTag() 的返回值直接调用了 contains("Purity")，但 getTag() 这次返回的是 null。这说明崩溃的本质不是 “NBT 数据太复杂”，而是“代码默认 tag 一定存在，但实际某些堆叠根本没有 tag”，于是空指针就出现了。
 * 中间我们也怀疑过是不是 FRMC 的配方优化把问题放大了。因为 FRMC 参考了 Fast-Recipe-Search-forge-1.20 的思路，而炉类配方查询又会经过 RecipeManagement -> Polymorph -> RecipeSelection 这条链。对照参考项目后可以确认，核心树搜索思路基本一致，真正不同的是 FRMC 额外接了 PolymorphIntegration、一些 ingredient 兼容层，以及更重的模组适配逻辑。所以更合理的判断是：FRMC 不是根因，但它更容易把这条有问题的配方路径跑出来。
 * 与此同时，另一个很自然的怀疑方向是 NBT。因为报错里直接提示 ItemStack.getTag() 返回了 null，而 Thirst 又在 cooking recipe 里去读 Purity 字段，所以当时完全可以合理地猜测：是不是某些带 NBT 的产物、或者某些特殊来源的食物/容器，进入了一个不该进入的匹配分支。这个猜测也解释了为什么问题是偶发的，而不是一进世界就必炸。
 * 再往后反编 Thirst 的实现，问题就完全坐实了。它的 WaterPurity 相关逻辑会把某些物品视作水容器，但这个判断并不保证 ItemStack 一定带 NBT；而 MixinAbstractCookingRecipe 却假设它一定有 tag。也就是说，这个 bug 的真实形态不是“有 NBT 就会崩”，而是“某些没有 tag 的相关物品进入了 Thirst 的 cooking recipe 分支后，判空缺失导致崩溃”。这也解释了为什么它是偶发的，因为触发条件取决于具体的物品来源、产物状态和调用时机。
 * 最后的修复策略就收束得很清楚了。与其去改 FRMC 的整套配方搜索语义，不如直接在 FRMC 里对 Thirst 的 AbstractCookingRecipe.matches() 做一个最小补丁，只把 ItemStack.getTag() 的空值兜住。这样不会改配方优先级，不会改 FRMC 的树搜索行为，也不会改变大多数正常 cooking recipe 的结果，只是把一个脆弱的 NPE 点补平。
 * 结论
 * - 直接坏点：Thirst was Taken
 * - 触发器：Polymorph
 * - 放大器：FRMC 的炉类配方查询链
 * - 早期怀疑：FRMC 自己的优化、导致任何带 NBT 的产物都会崩溃
 * - 最稳修法：补 Thirst 的判空，而不是改 FRMC 的配方逻辑
 * */
@Pseudo
@Mixin(net.minecraft.world.item.crafting.AbstractCookingRecipe.class)
public abstract class AbstractCookingRecipeMixin {

    @Redirect(
            method = "matches",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getTag()Lnet/minecraft/nbt/CompoundTag;"),
            require = 0
    )
    private CompoundTag frmc$safeTagLookup(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null && WaterPurity.isWaterFilledContainer(stack)) {
            return new CompoundTag();
        }
        return tag;
    }
}
