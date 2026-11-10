package com.y271727uy.FRMC.mixin.minecraft.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Map;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DispenserBlock.class)
public abstract class DispenserBehaviorRegistryMixin {
    @WrapOperation(method = "registerBehavior", at = @At(value = "INVOKE",
            target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", remap = false),
            require = 1)
    private static Object frmc$registerBehaviorUnderLock(Map<Item, DispenseItemBehavior> registry, Object item,
            Object behavior, Operation<Object> original) {
        // Forge can dispatch common setup for several mods on different workers.
        synchronized (registry) {
            return original.call(registry, item, behavior);
        }
    }
}
