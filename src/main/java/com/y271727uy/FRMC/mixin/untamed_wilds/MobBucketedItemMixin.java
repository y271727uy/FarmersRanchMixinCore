package com.y271727uy.FRMC.mixin.untamed_wilds;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import untamedwilds.util.EntityUtils;

@Pseudo
@Mixin(targets = "untamedwilds.item.MobBucketedItem", remap = false)
public abstract class MobBucketedItemMixin {
    @Shadow private Supplier<? extends EntityType<?>> entity;

    @Inject(method = "getSpecies", at = @At("RETURN"), cancellable = true, require = 0)
    private void frmc$clampInvalidBucketedMobSpecies(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int speciesCount = EntityUtils.getNumberOfSpecies(entity.get());
        if (speciesCount > 0) {
            cir.setReturnValue(Math.max(0, Math.min(cir.getReturnValue(), speciesCount - 1)));
        }
    }
}
