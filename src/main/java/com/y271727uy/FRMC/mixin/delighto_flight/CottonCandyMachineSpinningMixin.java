package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.block.entity.CottonCandyMachineBlockEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = CottonCandyMachineBlockEntity.class, remap = false)
public abstract class CottonCandyMachineSpinningMixin {
    @Unique private int frmc$lastSentProgress;

    @ModifyExpressionValue(method = "isPlayerStillValid", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;getPlayer(Ljava/util/UUID;)Lnet/minecraft/server/level/ServerPlayer;",
            remap = true))
    private ServerPlayer frmc$requireLocalNonSpectator(ServerPlayer player) {
        CottonCandyMachineBlockEntity machine = (CottonCandyMachineBlockEntity) (Object) this;
        if (player == null || player.isSpectator() || player.level() != machine.getLevel()) {
            return null;
        }
        return player;
    }

    @Inject(method = "stopSpinning", at = @At("HEAD"))
    private void frmc$resetLastSentProgress(CallbackInfo ci) {
        this.frmc$lastSentProgress = 0;
    }

    @Inject(method = "syncSpinProgress", at = @At("HEAD"), cancellable = true)
    private void frmc$syncChangedProgress(CallbackInfo ci) {
        int progress = ((CottonCandyMachineBlockEntity) (Object) this).getSpinTime();
        if (progress == this.frmc$lastSentProgress) {
            ci.cancel();
        } else {
            this.frmc$lastSentProgress = progress;
        }
    }
}
