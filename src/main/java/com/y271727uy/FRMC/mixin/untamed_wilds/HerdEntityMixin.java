package com.y271727uy.FRMC.mixin.untamed_wilds;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Accessor;
import untamedwilds.entity.ComplexMob;

import java.util.Iterator;
import java.util.List;

@Mixin(value = untamedwilds.entity.HerdEntity.class, remap = false)
public abstract class HerdEntityMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void frmc$validateHerdState(CallbackInfo ci) {
		List<ComplexMob> creatureList = this.frmc$getCreatureList();
		if (creatureList == null || creatureList.isEmpty()) {
			return;
		}

		for (Iterator<ComplexMob> iterator = creatureList.iterator(); iterator.hasNext(); ) {
			ComplexMob creature = iterator.next();
			if (creature == null || !creature.isAlive() || creature.isRemoved()) {
				iterator.remove();
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
			this.frmc$setRadius(8.0F);
		}

		if (this.frmc$getMaxHerdSize() <= 0) {
			this.frmc$setMaxHerdSize(creatureList.size());
		} else if (this.frmc$getMaxHerdSize() < creatureList.size()) {
			this.frmc$setMaxHerdSize(creatureList.size());
		}

		if (creatureList.size() >= this.frmc$getMaxHerdSize()) {
			this.frmc$setOpenToCombine(false);
		}
	}

	@Accessor("leader")
	abstract ComplexMob frmc$getLeader();

	@Accessor("leader")
	abstract void frmc$setLeader(ComplexMob leader);

	@Accessor("openToCombine")
	abstract boolean frmc$isOpenToCombine();

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
