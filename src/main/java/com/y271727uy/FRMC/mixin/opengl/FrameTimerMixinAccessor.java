package com.y271727uy.FRMC.mixin.opengl;

import net.minecraft.util.FrameTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(FrameTimer.class)
public interface FrameTimerMixinAccessor {
    @Accessor("logLength")
    int frmc$getLogLength();
}
