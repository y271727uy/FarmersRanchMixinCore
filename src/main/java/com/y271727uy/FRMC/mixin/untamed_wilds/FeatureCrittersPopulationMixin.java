package com.y271727uy.FRMC.mixin.untamed_wilds;

import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import untamedwilds.entity.ComplexMob;

@Pseudo
@Mixin(targets = "untamedwilds.world.gen.feature.FeatureCritters", remap = false)
public abstract class FeatureCrittersPopulationMixin {
	private static final int FRMC$MAX_LOCAL_MOBS = 16;

	@Inject(method = {"place", "m_142674_"}, at = @At("HEAD"), cancellable = true, require = 0)
	private void frmc$limitCritterFeatureDensity(FeaturePlaceContext<?> context,
			CallbackInfoReturnable<Boolean> cir) {
		AABB area = new AABB(context.origin()).inflate(32.0D, 16.0D, 32.0D);
		int nearby = context.level().getEntitiesOfClass(ComplexMob.class, area,
				ComplexMob::isAlive).size();
		if (nearby >= FRMC$MAX_LOCAL_MOBS) {
			cir.setReturnValue(false);
		}
	}
}
