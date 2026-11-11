package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.block.entity.CottonCandyMachineBlockEntity;
import com.cloudmeow.delightoflight.network.DFNetworking.StopSpinningMessage;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = StopSpinningMessage.class, remap = false)
public abstract class StopSpinningMessageMixin {
    @WrapOperation(method = "lambda$handle$0", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            remap = true))
    private static BlockEntity frmc$validateStop(ServerLevel level, BlockPos pos, Operation<BlockEntity> original, @Local ServerPlayer player) {
        if (!player.canReach(pos, 0.0D) || !level.hasChunkAt(pos)) {
            return null;
        }
        BlockEntity blockEntity = original.call(level, pos);
        return blockEntity instanceof CottonCandyMachineBlockEntity machine
                && player.getUUID().equals(machine.getSpinningPlayerId()) ? machine : null;
    }
}
