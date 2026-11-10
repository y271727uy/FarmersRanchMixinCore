package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.living.TremorzillaEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TremorzillaEntity.class)
public abstract class TremorzillaBlockScanMixin {
    @WrapOperation(method = "breakBlocksAround", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
            require = 1)
    private BlockState frmc$skipOutsideBreakSphere(Level level, BlockPos pos, Operation<BlockState> original,
            @Local(argsOnly = true) Vec3 center, @Local(argsOnly = true, ordinal = 0) float radius,
            @Local(argsOnly = true, ordinal = 0) boolean square) {
        if (!square && pos.distToCenterSqr(center.x, center.y, center.z) >= radius * radius) {
            return Blocks.AIR.defaultBlockState();
        }
        return original.call(level, pos);
    }
}
