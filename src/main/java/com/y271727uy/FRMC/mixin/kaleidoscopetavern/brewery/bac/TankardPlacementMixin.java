package com.y271727uy.FRMC.mixin.kaleidoscopetavern.brewery.bac;

import com.y271727uy.FRMC.integration.kaleidoscopetavern.TavernMugs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import umpaz.brewinandchewin.common.registry.BnCItems;

@Mixin(value = BnCItems.class, remap = false)
public abstract class TankardPlacementMixin {
    @Inject(method = "lambda$static$4", at = @At("HEAD"), cancellable = true)
    private static void frmc$bindTankardToMug(CallbackInfoReturnable<Item> cir) {
        Block mug = ForgeRegistries.BLOCKS.getValue(TavernMugs.EMPTY_ID);
        if (mug != null && mug != Blocks.AIR) {
            cir.setReturnValue(new TavernMugs.TankardItem(mug));
        }
    }
}
