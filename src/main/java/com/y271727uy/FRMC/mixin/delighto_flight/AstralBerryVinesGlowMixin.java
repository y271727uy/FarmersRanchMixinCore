package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.block.AstralBerryVinesBlock;
import com.y271727uy.FRMC.integration.delighto_flight.AstralBerryGlowCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = AstralBerryVinesBlock.class, remap = false)
public abstract class AstralBerryVinesGlowMixin {
    @Inject(method = {"tick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
            "m_213897_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"},
            at = @At(value = "INVOKE", ordinal = 0,
                    target = "Lcom/cloudmeow/delightoflight/block/AstralBerryVines;applyGlow(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V"),
            cancellable = true, require = 1)
    private void frmc$skipUnchangedGlowScan(BlockState state, ServerLevel level, BlockPos head,
                                           RandomSource random, CallbackInfo ci) {
        // DF and vanilla have already checked that this head is still present and supported.
        if (!AstralBerryGlowCache.shouldScan(level, head)) {
            level.scheduleTick(head, state.getBlock(), 20);
            ci.cancel();
        }
    }
}
