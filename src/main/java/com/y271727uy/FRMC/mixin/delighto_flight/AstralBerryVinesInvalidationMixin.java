package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.block.AstralBerryVines;
import com.y271727uy.FRMC.integration.delighto_flight.AstralBerryGlowCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class AstralBerryVinesInvalidationMixin {
    @Inject(method = "onBlockStateChange", at = @At("HEAD"))
    private void frmc$invalidateChangedVines(BlockPos pos, BlockState previous, BlockState next, CallbackInfo ci) {
        if (previous.getBlock() instanceof AstralBerryVines || next.getBlock() instanceof AstralBerryVines) {
            AstralBerryGlowCache.invalidateColumn((ServerLevel) (Object) this, pos);
        }
    }
}
