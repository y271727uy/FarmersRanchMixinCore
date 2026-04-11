package com.y271727uy.FRMC.mixin.untamed_wilds;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import untamedwilds.entity.ComplexMob;

import java.util.List;

@Pseudo
@Mixin(targets = "untamedwilds.entity.HerdEntity", remap = false)
public abstract class HerdEntityMixin {
	@Unique
	private static final float FRMC$DEFAULT_RADIUS = 8.0F;
	@Unique
	private static final int FRMC$DEFAULT_MAX_HERD_SIZE = 8;
	@Unique
	private static final int FRMC$COMBINE_SCAN_STRIDE = 2;
	@Unique
	private int frmc$combineScanTicker;

	@Inject(method = "tick", at = @At("HEAD"))
	private void frmc$validateHerdState(CallbackInfo ci) {
		List<ComplexMob> creatureList = this.frmc$getCreatureList();
		if (creatureList == null || creatureList.isEmpty()) {
			return;
		}

		for (int index = creatureList.size() - 1; index >= 0; index--) {
			ComplexMob creature = creatureList.get(index);
			if (creature == null || !creature.isAlive() || creature.isRemoved()) {
				creatureList.remove(index);
			}
		}

		if (creatureList.isEmpty()) {
			return;
		}

		ComplexMob leader = this.frmc$getLeader();
		if (leader == null || !leader.isAlive() || leader.isRemoved()) {
			this.frmc$setLeader(creatureList.get(0));
			leader = this.frmc$getLeader();
		}

		if (leader == null) {
			return;
		}

		if (!creatureList.contains(leader)) {
			creatureList.add(leader);
		}

		if (this.frmc$getRadius() <= 0.0F) {
			this.frmc$setRadius(FRMC$DEFAULT_RADIUS);
		}

		if (this.frmc$getMaxHerdSize() <= 0) {
			this.frmc$setMaxHerdSize(FRMC$DEFAULT_MAX_HERD_SIZE);
		}

		if (creatureList.size() >= this.frmc$getMaxHerdSize()) {
			this.frmc$setOpenToCombine(false);
		}
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void frmc$throttleCombineScan(CallbackInfo ci) {
		List<ComplexMob> creatureList = this.frmc$getCreatureList();
		if (creatureList == null || creatureList.isEmpty() || creatureList.size() >= this.frmc$getMaxHerdSize()) {
			return;
		}

		this.frmc$combineScanTicker++;
		if (this.frmc$combineScanTicker % FRMC$COMBINE_SCAN_STRIDE != 0) {
			this.frmc$setOpenToCombine(false);
		}
	}

	@Accessor("leader")
	abstract ComplexMob frmc$getLeader();

	@Accessor("leader")
	abstract void frmc$setLeader(ComplexMob leader);


	@Accessor("openToCombine")
	abstract void frmc$setOpenToCombine(boolean openToCombine);

	@Accessor("radius")
	abstract float frmc$getRadius();

	@Accessor("radius")
	abstract void frmc$setRadius(float radius);

	@Accessor("maxHerdSize")
	abstract int frmc$getMaxHerdSize();

	@Accessor("maxHerdSize")
	abstract void frmc$setMaxHerdSize(int maxHerdSize);

	@Accessor("creatureList")
	abstract List<ComplexMob> frmc$getCreatureList();
}
