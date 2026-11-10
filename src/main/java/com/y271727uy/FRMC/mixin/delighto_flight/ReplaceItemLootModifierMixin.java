package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.loot.ReplaceItemLootModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.Consumer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(value = ReplaceItemLootModifier.class, remap = false)
public abstract class ReplaceItemLootModifierMixin {
    @Shadow @Final private Item removedItem;

    // 与提交pr重合
    @Redirect(method = "doApply", at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;forEach(Ljava/util/function/Consumer;)V"))
    private void frmc$removeAllMatchingDrops(ObjectArrayList<ItemStack> loot, Consumer<ItemStack> ignored) {
        loot.removeIf(stack -> stack.is(this.removedItem));
    }
}
