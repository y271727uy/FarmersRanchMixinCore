package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = EntityLeafcutterAnt.class, remap = false)
public abstract class EntityLeafcutterAntPopulationMixin {
	private static final int FRMC$MAX_ACTIVE_ANTS = 10;

	@WrapOperation(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"), require = 0)
	private boolean frmc$capQueenOffspring(Level level, Entity entity, Operation<Boolean> original) {
		EntityLeafcutterAnt queen = (EntityLeafcutterAnt) (Object) this;
		long activeAnts = level.getEntitiesOfClass(EntityLeafcutterAnt.class,
				queen.getBoundingBox().inflate(24.0D, 12.0D, 24.0D), Entity::isAlive).size();
		if (activeAnts >= FRMC$MAX_ACTIVE_ANTS) {
			return false;
		}
		return original.call(level, entity);
	}
}
