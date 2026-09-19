package com.y271727uy.FRMC.mixin.vinery.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.capability.displaywine.client.DisplayWineSafeState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.satisfy.vinery.client.render.block.storage.BigBottleRenderer;
import net.satisfy.vinery.client.render.block.storage.FourBottleRenderer;
import net.satisfy.vinery.client.render.block.storage.NineBottleRenderer;
import net.satisfy.vinery.client.render.block.storage.WineBoxRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = {BigBottleRenderer.class, FourBottleRenderer.class, NineBottleRenderer.class, WineBoxRenderer.class}, remap = false)
public abstract class SafeBottleRendererMixin {
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;setValue(Lnet/minecraft/world/level/block/state/properties/Property;Ljava/lang/Comparable;)Ljava/lang/Object;",
                    remap = true),
            require = 0)
    private Object frmc$safelySetFakeModel(
            BlockState state,
            Property<?> property,
            Comparable<?> value,
            Operation<Object> original) {
        return DisplayWineSafeState.safeSetFakeModel(state, property, value);
    }
}
