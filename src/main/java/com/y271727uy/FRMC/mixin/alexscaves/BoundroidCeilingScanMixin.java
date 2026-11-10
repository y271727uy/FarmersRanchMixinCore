package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.living.BoundroidWinchEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BoundroidWinchEntity.class, remap = false)
public abstract class BoundroidCeilingScanMixin {
    @Unique private Level frmc$ceilingLevel;
    @Unique private BlockPos frmc$ceilingScanOrigin;
    @Unique private BlockPos frmc$ceilingBlock;
    @Unique private int frmc$ceilingScanTick;

    @WrapOperation(method = "calculateDistanceToCeiling", at = @At(value = "INVOKE", target =
            "Lcom/github/alexmodguy/alexscaves/server/entity/living/BoundroidWinchEntity;getCeilingOf(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"),
            require = 1)
    private BlockPos frmc$reuseCeilingBlock(BoundroidWinchEntity winch, BlockPos origin, Operation<BlockPos> original) {
        Level level = winch.level();
        int tick = winch.tickCount;
        if (level == frmc$ceilingLevel && origin.equals(frmc$ceilingScanOrigin) && tick - frmc$ceilingScanTick == 1) {
            return frmc$ceilingBlock;
        }
        BlockPos ceiling = original.call(winch, origin);
        frmc$ceilingLevel = level;
        frmc$ceilingScanOrigin = origin;
        frmc$ceilingScanTick = tick;
        frmc$ceilingBlock = ceiling;
        return ceiling;
    }
}
