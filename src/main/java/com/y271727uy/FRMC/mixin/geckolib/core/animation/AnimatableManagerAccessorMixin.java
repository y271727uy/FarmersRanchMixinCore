package com.y271727uy.FRMC.mixin.geckolib.core.animation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;
import software.bernie.geckolib.core.animation.AnimatableManager;

@Pseudo
@Mixin(value = AnimatableManager.class, remap = false)
public interface AnimatableManagerAccessorMixin {
    @Invoker(value = "finishFirstTick", remap = false)
    void gbf$finishFirstTick();
}



