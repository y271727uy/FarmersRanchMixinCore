package com.y271727uy.FRMC.mixin.minecraft.blockchecking.timing;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkKind;
import com.y271727uy.FRMC.capability.blockchecking.server.BlockCheckingRuntime;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$RebindableTickingBlockEntityWrapper")
public abstract class BlockEntityTickTimingMixin {
    @WrapMethod(method = "tick()V", require = 0)
    private void frmc$trackBlockEntityTick(Operation<Void> original) {
        long token = BlockCheckingRuntime.enterWork(WorkKind.BLOCK_ENTITY_TICK);
        try {
            original.call();
        } finally {
            BlockCheckingRuntime.exitWork(token);
        }
    }
}
