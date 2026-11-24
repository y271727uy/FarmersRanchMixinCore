package com.y271727uy.FRMC.mixin.vitalherbs;

import net.mcreator.vitalherbs.block.TeapotBlock;
import net.mcreator.vitalherbs.procedures.TeapotOnTickUpdateProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TeapotOnTickUpdateProcedure.class, remap = false)
public abstract class TeapotBrewingMixin {
    // In Vital Herbs 6.1.1, this heat check follows bucket handling and starts the recipe checks.
    @Inject(
        method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDD)V",
        at = @At(value = "CONSTANT", args = "stringValue=heat", ordinal = 4),
        cancellable = true,
        require = 1
    )
    private static void frmc$disableHardcodedBrewing(LevelAccessor level, double x, double y, double z, CallbackInfo ci) {
        ci.cancel();
        if (level.isClientSide()) {
            return;
        }

        BlockPos pos = BlockPos.containing(x, y, z);
        BlockEntity teapot = level.getBlockEntity(pos);
        if (teapot != null) {
            CompoundTag data = teapot.getPersistentData();
            if (data.getDouble("time") != 0) {
                data.putDouble("time", 0);
                teapot.setChanged();
            }
        }

        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(TeapotBlock.COOKING) && state.getValue(TeapotBlock.COOKING)) {
            level.setBlock(pos, state.setValue(TeapotBlock.COOKING, false), Block.UPDATE_ALL);
        }
    }
}
