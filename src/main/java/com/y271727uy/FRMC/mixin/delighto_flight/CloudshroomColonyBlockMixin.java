package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.block.CloudshroomColonyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = CloudshroomColonyBlock.class, remap = false)
public abstract class CloudshroomColonyBlockMixin {
    @Inject(method = "tick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"), cancellable = true)
    private void frmc$skipUnchangedWeather(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        int weather = level.isThundering() ? 2 : (level.isRaining() ? 1 : 0);
        if (state.getValue(CloudshroomColonyBlock.WEATHER_AGE) == weather) {
            ci.cancel();
        }
    }
}
