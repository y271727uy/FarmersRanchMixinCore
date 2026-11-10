package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.client.event.ClientEvents;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.y271727uy.FRMC.integration.alexscaves.ClientBiomeSampleCache;
import com.y271727uy.frmc.mixinfabricextras.injector.FabricInject;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientEvents.class, remap = false)
public abstract class ClientEventsBiomeSamplingMixin {
    @FabricInject(method = "onClientTick", at = @At(value = "INVOKE",
            target = "Lcom/github/alexmodguy/alexscaves/server/level/biome/ACBiomeRegistry;calculateBiomeSkyOverride(Lnet/minecraft/world/entity/Entity;)F"),
            remap = false, require = 1)
    private void frmc$beginBiomeSampling(TickEvent.ClientTickEvent event, CallbackInfo ci) {
        ClientBiomeSampleCache.begin();
    }

    @WrapMethod(method = "onClientTick")
    private void frmc$clearBiomeSamples(TickEvent.ClientTickEvent event, Operation<Void> original) {
        try {
            original.call(event);
        } finally {
            ClientBiomeSampleCache.end();
        }
    }
}
