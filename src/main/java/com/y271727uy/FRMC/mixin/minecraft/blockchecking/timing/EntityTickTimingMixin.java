package com.y271727uy.FRMC.mixin.minecraft.blockchecking.timing;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkKind;
import com.y271727uy.FRMC.capability.blockchecking.server.BlockCheckingRuntime;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public abstract class EntityTickTimingMixin {
    @WrapMethod(method = "tick()V", require = 0)
    private void frmc$trackEntityTick(Operation<Void> original) {
        long token = BlockCheckingRuntime.enterWork(WorkKind.ENTITY_TICK);
        try {
            original.call();
        } finally {
            BlockCheckingRuntime.exitWork(token);
        }
    }
}
