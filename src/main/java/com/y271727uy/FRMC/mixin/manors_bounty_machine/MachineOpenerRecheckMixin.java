package com.y271727uy.FRMC.mixin.manors_bounty_machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "net.zhaiji.manorsbountymachine.block.entity.BlenderBlockEntity",
        "net.zhaiji.manorsbountymachine.block.entity.FermenterBlockEntity",
        "net.zhaiji.manorsbountymachine.block.entity.OvenBlockEntity",
        "net.zhaiji.manorsbountymachine.block.entity.StockPotBlockEntity",
        "net.zhaiji.manorsbountymachine.block.entity.TeapotBlockEntity"
}, remap = false)
public abstract class MachineOpenerRecheckMixin extends BlockEntity {
    @Shadow
    @Final
    public ContainerOpenersCounter openersCounter;

    protected MachineOpenerRecheckMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "recheckOpen()V", at = @At("HEAD"), cancellable = true, require = 0)
    private void frmc$staggerIdleOpenerRechecks(CallbackInfo ci) {
        Level level = this.getLevel();
        // startOpen and stopOpen update the count immediately. Keep a periodic check for a missed open.
        if (level != null && this.openersCounter.getOpenerCount() == 0
                && (level.getGameTime() + this.getBlockPos().hashCode()) % 16L != 0L) {
            ci.cancel();
        }
    }
}
