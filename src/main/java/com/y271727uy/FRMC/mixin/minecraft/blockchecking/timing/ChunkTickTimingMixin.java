package com.y271727uy.FRMC.mixin.minecraft.blockchecking.timing;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkKind;
import com.y271727uy.FRMC.capability.blockchecking.server.BlockCheckingRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerLevel.class)
public abstract class ChunkTickTimingMixin {
    @WrapMethod(method = "tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V", require = 0)
    private void frmc$trackChunkTick(LevelChunk chunk, int randomTickSpeed, Operation<Void> original) {
        long token = BlockCheckingRuntime.enterWork(WorkKind.CHUNK_TICK);
        try {
            original.call(chunk, randomTickSpeed);
        } finally {
            BlockCheckingRuntime.exitWork(token);
        }
    }
}
