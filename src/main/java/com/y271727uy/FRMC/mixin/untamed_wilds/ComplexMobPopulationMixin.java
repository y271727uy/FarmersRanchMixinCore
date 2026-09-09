package com.y271727uy.FRMC.mixin.untamed_wilds;

import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import untamedwilds.entity.ComplexMob;

/** Applies a shared local population guard to species without one of their own. */
@Pseudo
@Mixin(value = ComplexMob.class, remap = false)
public abstract class ComplexMobPopulationMixin {
	private static final int FRMC$MAX_LOCAL_SPECIES = 16;

	@Inject(method = "wantsToBreed", at = @At("HEAD"), cancellable = true, require = 0)
	private void frmc$limitLocalSpeciesPopulation(CallbackInfoReturnable<Boolean> cir) {
		ComplexMob mob = (ComplexMob) (Object) this;
		AABB area = mob.getBoundingBox().inflate(24.0D, 12.0D, 24.0D);
		long nearby = mob.level().getEntitiesOfClass(ComplexMob.class, area,
				entity -> entity.isAlive() && entity.getType() == mob.getType()).size();
		if (nearby >= FRMC$MAX_LOCAL_SPECIES) {
			cir.setReturnValue(false);
		}
	}
}
