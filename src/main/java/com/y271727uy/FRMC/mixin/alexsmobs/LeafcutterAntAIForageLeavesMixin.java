package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.ai.LeafcutterAntAIForageLeaves;
import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = LeafcutterAntAIForageLeaves.class, remap = false)
public abstract class LeafcutterAntAIForageLeavesMixin {
	/** The vanilla goal doubles this value when an ant has a hive, so 5 yields a hard maximum of 10. */
	@Shadow @Final @Mutable
	private int searchRange;
	@Shadow @Final
	private EntityLeafcutterAnt ant;
	@Unique
	private int frmc$searchCooldown;

	@Inject(method = "<init>", at = @At("RETURN"), require = 0)
	private void frmc$capLeafSearchRange(EntityLeafcutterAnt ant, CallbackInfo ci) {
		this.searchRange = 5;
		this.frmc$searchCooldown = ant.getRandom().nextInt(101);
	}

	@Inject(method = "canUse", at = @At("HEAD"), cancellable = true, require = 0)
	private void frmc$throttleLeafSearch(CallbackInfoReturnable<Boolean> cir) {
		if (this.frmc$searchCooldown > 0) {
			this.frmc$searchCooldown--;
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "canUse", at = @At("RETURN"), require = 0)
	private void frmc$setLeafSearchCooldown(CallbackInfoReturnable<Boolean> cir) {
		if (this.frmc$searchCooldown == 0) {
			this.frmc$searchCooldown = cir.getReturnValue() ? 200 : 500;
		}
	}

	@Inject(method = "shouldRecalculatePath", at = @At("RETURN"), cancellable = true, require = 0)
	private void frmc$throttlePathRecalculation(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && (this.ant.tickCount % 60) != 0) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "breakLeaves", at = @At("HEAD"), cancellable = true, require = 0)
	private void frmc$throttleLeafBreaking(CallbackInfo ci) {
		if ((this.ant.tickCount % 100) != 0) {
			ci.cancel();
		}
	}
}
