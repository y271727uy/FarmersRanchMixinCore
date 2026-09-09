package com.y271727uy.FRMC.mixin.alexsmobs;

import net.minecraft.world.entity.Entity;
import com.github.alexthe666.alexsmobs.entity.EntityCatfish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityCatfish$TargetFoodGoal", remap = false)
public abstract class CatfishTargetFoodGoalMixin {
	private static final int FRMC$FAILED_SEARCH_COOLDOWN = 500;
	private static final int FRMC$SUCCESS_SEARCH_COOLDOWN = 200;

	@Shadow
	private int executionCooldown;
	@Shadow
	private Entity food;
	@Shadow
	private EntityCatfish catfish;

	@Inject(method = "canUse", at = @At("HEAD"), cancellable = true, require = 0)
	private void frmc$disableLargeCatfishHunting(CallbackInfoReturnable<Boolean> cir) {
		if (this.catfish.getCatfishSize() == 2) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "canUse", at = @At("RETURN"), require = 0)
	private void frmc$throttleFoodSearch(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && this.food != null) {
			this.executionCooldown = Math.max(this.executionCooldown, FRMC$SUCCESS_SEARCH_COOLDOWN);
		} else if (this.food == null) {
			this.executionCooldown = Math.max(this.executionCooldown, FRMC$FAILED_SEARCH_COOLDOWN);
		}
	}
}
