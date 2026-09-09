package com.y271727uy.FRMC.mixin.starlight;

import ca.spottedleaf.starlight.common.world.ExtendedWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Makes Create's synthetic render world discoverable by Starlight. */
@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld")
public abstract class VirtualRenderWorldMixin implements ExtendedWorld {
}
