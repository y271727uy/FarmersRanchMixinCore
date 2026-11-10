package com.y271727uy.FRMC.mixin.alexscaves;

import net.minecraftforge.common.world.ForgeChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ForgeChunkManager.TicketOwner.class, remap = false)
public interface ForgeTicketOwnerInvoker {
    @Invoker("<init>")
    @SuppressWarnings("rawtypes")
    static ForgeChunkManager.TicketOwner frmc$entityTicketOwner(String modId, Comparable<?> entityId) {
        throw new AssertionError();
    }
}
