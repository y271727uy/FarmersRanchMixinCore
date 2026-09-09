package com.y271727uy.FRMC.mixin.alexsmobs;

import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityCatfish$FascinateLanternGoal", remap = false)
public abstract class CatfishFascinateLanternGoalMixin {
	private static final int FRMC$FAILED_SEARCH_COOLDOWN = 500;
	private static final int FRMC$SUCCESS_SEARCH_COOLDOWN = 200;

	@Shadow
	private int runDelay;
	@Shadow
	private BlockPos destinationBlock;

	@Inject(method = "canUse", at = @At("RETURN"), cancellable = false, require = 0)
	private void frmc$throttleLanternSearch(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			this.runDelay = Math.max(this.runDelay, FRMC$SUCCESS_SEARCH_COOLDOWN);
		} else if (this.destinationBlock == null) {
			this.runDelay = Math.max(this.runDelay, FRMC$FAILED_SEARCH_COOLDOWN);
		}
	}
}
