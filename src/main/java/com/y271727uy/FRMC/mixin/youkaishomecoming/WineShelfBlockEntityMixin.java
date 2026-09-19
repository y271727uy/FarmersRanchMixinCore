package com.y271727uy.FRMC.mixin.youkaishomecoming;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.y271727uy.FRMC.capability.displaywine.DisplayWineBottles;
import dev.xkmc.youkaishomecoming.content.pot.storage.shelf.WineShelfBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = WineShelfBlockEntity.class, remap = false)
public abstract class WineShelfBlockEntityMixin {
    @ModifyReturnValue(method = "isFlask", at = @At("RETURN"), require = 0)
    private static boolean frmc$allowTaggedBottles(boolean original, ItemStack stack) {
        return original || DisplayWineBottles.isSmallBottle(stack);
    }
}
