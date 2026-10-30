package com.y271727uy.FRMC.mixin.sereneseasons;

import com.y271727uy.FRMC.integration.sereneseasons.SeasonalCropBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets Serene Seasons apply its seasonal growth decision to FRMC data groups.
 * The final growth cancellation remains in Serene Seasons' callback chain.
 */
@Pseudo
@Mixin(targets = "sereneseasons.season.SeasonalCropGrowthHandler", remap = false)
public abstract class SeasonalCropGrowthHandlerMixin {
    @Inject(
        method = "onCropGrowth(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 0
    )
    private static void frmc$handleDataGroupGrowth(
        Level level,
        BlockPos pos,
        BlockState state,
        CallbackInfo originalCallback,
        CallbackInfo injectionCallback
    ) {
        if (!level.isClientSide && !SeasonalCropBridge.isGrowthAllowed(level, pos, state)) {
            originalCallback.cancel();
        }
    }
}
