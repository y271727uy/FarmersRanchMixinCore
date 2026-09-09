package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityCatfish;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = EntityCatfish.class, remap = false)
public abstract class EntityCatfishPerformanceMixin {
	@Unique
	private static final int FRMC$LANTERN_SCAN_INTERVAL = 200;
	@Unique
	private static final int FRMC$SPIT_COOLDOWN = 1200;
	@Unique
	private int frmc$lanternScanCountdown;
	@Unique
	private boolean frmc$allowLanternScan;
	@Unique
	private int frmc$spitCountdown;

	@Inject(method = "aiStep", at = @At("HEAD"), require = 0)
	private void frmc$throttleLanternScan(CallbackInfo ci) {
		if (this.frmc$spitCountdown > 0) {
			this.frmc$spitCountdown--;
		}
		if (this.frmc$lanternScanCountdown > 0) {
			this.frmc$lanternScanCountdown--;
			this.frmc$allowLanternScan = false;
			return;
		}
		this.frmc$lanternScanCountdown = FRMC$LANTERN_SCAN_INTERVAL - 1;
		this.frmc$allowLanternScan = true;
	}

	@WrapOperation(method = "aiStep", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), require = 0)
	private BlockState frmc$skipLanternBlockLookup(Level level, BlockPos pos, Operation<BlockState> original) {
		return this.frmc$allowLanternScan ? original.call(level, pos) : Blocks.AIR.defaultBlockState();
	}

	@Inject(method = "spit", at = @At("HEAD"), cancellable = true, require = 0)
	private void frmc$limitSpitFrequency(CallbackInfo ci) {
		EntityCatfish catfish = (EntityCatfish) (Object) this;
		if (this.frmc$spitCountdown > 0 || !catfish.canSpit()) {
			ci.cancel();
			return;
		}
		this.frmc$spitCountdown = FRMC$SPIT_COOLDOWN;
	}

	@Inject(method = "removeWhenFarAway", at = @At("RETURN"), cancellable = true, require = 0)
	private void frmc$preventWildCatfishPersistence(double distance, CallbackInfoReturnable<Boolean> cir) {
		EntityCatfish catfish = (EntityCatfish) (Object) this;
		if (!catfish.fromBucket()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "swallowEntity", at = @At("HEAD"), cancellable = true, require = 0)
	private void frmc$disableMobSwallowing(net.minecraft.world.entity.Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof Mob) {
			cir.setReturnValue(false);
		}
	}
}
