package com.y271727uy.FRMC.mixin.alexsmobs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Spreads expensive item/entity target scans across ticks for the three foraging birds. */
@Pseudo
@Mixin(targets = {
		"com.github.alexthe666.alexsmobs.entity.EntityCrow$AITargetItems",
		"com.github.alexthe666.alexsmobs.entity.EntityCrow$AIScatter",
		"com.github.alexthe666.alexsmobs.entity.EntityBlueJay$AITargetItems",
		"com.github.alexthe666.alexsmobs.entity.EntityBlueJay$AIScatter",
		"com.github.alexthe666.alexsmobs.entity.EntitySeagull$AITargetItems",
		"com.github.alexthe666.alexsmobs.entity.EntitySeagull$AIScatter"
}, remap = false)
public abstract class BirdTargetSearchCooldownMixin {
	@Unique
	private int frmc$targetSearchCooldown;

	@Inject(method = "canUse", at = @At("HEAD"), cancellable = true, require = 0)
	private void frmc$throttleTargetSearch(CallbackInfoReturnable<Boolean> cir) {
		if (this.frmc$targetSearchCooldown > 0) {
			this.frmc$targetSearchCooldown--;
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "canUse", at = @At("RETURN"), require = 0)
	private void frmc$setTargetSearchCooldown(CallbackInfoReturnable<Boolean> cir) {
		if (this.frmc$targetSearchCooldown == 0) {
			this.frmc$targetSearchCooldown = cir.getReturnValue()
					? 100 + ((this.hashCode() & 0x7F) % 101)
					: 200 + ((this.hashCode() >>> 7) % 201);
		}
	}
}
