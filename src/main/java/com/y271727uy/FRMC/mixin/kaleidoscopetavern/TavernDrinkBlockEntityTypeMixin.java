package com.y271727uy.FRMC.mixin.kaleidoscopetavern;

import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.TavernBarrelWine;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.TavernMugs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityType.class)
public abstract class TavernDrinkBlockEntityTypeMixin {
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void frmc$allowDrinkDisplay(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (ModBlocks.DRINK_BE.isPresent() && (Object) this == ModBlocks.DRINK_BE.get()
                && (state.getBlock() instanceof TavernMugs.FilledBlock || TavernBarrelWine.isCocktailDisplay(state))) {
            cir.setReturnValue(true);
        }
    }
}
