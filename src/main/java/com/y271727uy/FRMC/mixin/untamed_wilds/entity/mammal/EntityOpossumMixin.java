package com.y271727uy.FRMC.mixin.untamed_wilds.entity.mammal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import untamedwilds.entity.mammal.EntityOpossum;

/** Prevents natural breeding from creating an unbounded local critter colony. */
@Pseudo
@Mixin(targets = "untamedwilds.entity.mammal.EntityOpossum", remap = false)
public abstract class EntityOpossumMixin {
	private static final int FRMC$MAX_LOCAL_OPOSSUMS = 16;

	@Inject(method = "wantsToBreed", at = @At("HEAD"), cancellable = true, require = 0)
	private void frmc$limitLocalPopulation(CallbackInfoReturnable<Boolean> cir) {
		EntityOpossum opossum = (EntityOpossum) (Object) this;
		AABB area = opossum.getBoundingBox().inflate(16.0D, 8.0D, 16.0D);
		long nearby = opossum.level().getEntitiesOfClass(EntityOpossum.class, area,
				Entity::isAlive).size();
		if (nearby >= FRMC$MAX_LOCAL_OPOSSUMS) {
			cir.setReturnValue(false);
		}
	}
}
