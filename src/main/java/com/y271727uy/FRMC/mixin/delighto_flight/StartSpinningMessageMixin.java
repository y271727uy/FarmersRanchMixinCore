package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.network.DFNetworking.StartSpinningMessage;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = StartSpinningMessage.class, remap = false)
public abstract class StartSpinningMessageMixin {
    @WrapOperation(method = "lambda$handle$0", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            remap = true))
    private static BlockEntity frmc$validateStart(ServerLevel level, BlockPos pos, Operation<BlockEntity> original, @Local ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !player.getMainHandItem().is(Items.STICK)
                || !player.canReach(pos, 0.0D) || !level.hasChunkAt(pos) || !level.mayInteract(player, pos)) {
            return null;
        }
        HitResult hit = player.pick(5.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
                || !blockHit.getBlockPos().equals(pos)) {
            return null;
        }
        return original.call(level, pos);
    }
}
