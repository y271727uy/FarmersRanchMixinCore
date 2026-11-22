package com.y271727uy.FRMC.mixin.brewinandchewin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.integration.brewinandchewin.KegWorldPouring;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import umpaz.brewinandchewin.common.block.KegBlock;
import umpaz.brewinandchewin.common.block.entity.KegBlockEntity;

@Mixin(value = KegBlock.class, remap = false)
public abstract class KegBlockInteractionMixin {
    @WrapOperation(method = {"use", "m_6227_"}, at = @At(value = "INVOKE",
            target = "Lumpaz/brewinandchewin/common/block/entity/KegBlockEntity;"
                    + "extractInWorld(Lnet/minecraft/world/item/ItemStack;IZ)Ljava/util/List;"))
    private List<ItemStack> frmc$requireSneakToTakeLiquid(KegBlockEntity keg, ItemStack input, int limit,
                                                        boolean creative, Operation<List<ItemStack>> original,
                                                        @Local(argsOnly = true) Player player) {
        if (player.isShiftKeyDown()) {
            return original.call(keg, input, limit, creative);
        }
        return ((KegWorldPouring) keg).frmc$pourIntoKeg(input, limit, creative);
    }
}
