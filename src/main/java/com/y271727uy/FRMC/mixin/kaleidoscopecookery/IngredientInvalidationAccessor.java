package com.y271727uy.FRMC.mixin.kaleidoscopecookery;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Ingredient.class)
public interface IngredientInvalidationAccessor {
    @Accessor(value = "INVALIDATION_COUNTER", remap = false)
    static AtomicInteger frmc$getInvalidationCounter() {
        throw new AssertionError();
    }
}
