package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.block.entity.CottonCandyMachineBlockEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
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

    // 与提交pr重合
    @ModifyExpressionValue(method = "isPlayerStillValid", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;getPlayer(Ljava/util/UUID;)Lnet/minecraft/server/level/ServerPlayer;",
            remap = true))
    private ServerPlayer frmc$requireLocalPlayerHoldingStick(ServerPlayer player) {
        CottonCandyMachineBlockEntity machine = (CottonCandyMachineBlockEntity) (Object) this;
        if (player == null || player.isSpectator() || player.level() != machine.getLevel()
                || !player.getMainHandItem().is(Items.STICK)) {
            return null;
        }
        return player;
    }

    // 与提交pr重合
    @Inject(method = "spinningTick", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lcom/cloudmeow/delightoflight/block/entity/CottonCandyMachineBlockEntity;spinTime:I"),
            cancellable = true)
    private static void frmc$stopWhenRecipeDisappears(Level level, BlockPos pos, BlockState state,
                                                       CottonCandyMachineBlockEntity machine, CallbackInfo ci) {
        // This write belongs to the no-recipe branch. Stop also syncs the cleared owner and output.
        machine.stopSpinning();
        ci.cancel();
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
