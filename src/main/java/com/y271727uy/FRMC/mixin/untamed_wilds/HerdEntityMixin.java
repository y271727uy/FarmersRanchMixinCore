package com.y271727uy.FRMC.mixin.untamed_wilds;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import untamedwilds.entity.ComplexMob;

import java.lang.reflect.Field;
import java.util.ArrayList;
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
	private int frmc$combineScanTicker = FRMC$COMBINE_SCAN_STRIDE;
	@Unique
	private static volatile boolean FRMC$FIELDS_RESOLVED;
	@Unique
	private static Field FRMC$LEADER_FIELD;
	@Unique
	private static Field FRMC$OPEN_TO_COMBINE_FIELD;
	@Unique
	private static Field FRMC$RADIUS_FIELD;
	@Unique
	private static Field FRMC$MAX_HERD_SIZE_FIELD;
	@Unique
	private static Field FRMC$CREATURE_LIST_FIELD;

	@Inject(method = "tick", at = @At("HEAD"))
	private void frmc$validateHerdState(CallbackInfo ci) {
		List<ComplexMob> creatureList = this.frmc$getCreatureList();
		if (creatureList == null) {
			return;
		}
		int creatureCount = creatureList.size();
		if (creatureCount == 0) {
			return;
		}

		for (int index = creatureCount - 1; index >= 0; index--) {
			ComplexMob creature = creatureList.get(index);
			if (creature == null || !creature.isAlive() || creature.isRemoved()) {
				creatureList.remove(index);
			}
		}

		creatureCount = creatureList.size();
		if (creatureCount == 0) {
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
			creatureCount++;
		}
		int maxHerdSize = this.frmc$getMaxHerdSize();

		if (this.frmc$getRadius() <= 0.0F) {
			this.frmc$setRadius(FRMC$DEFAULT_RADIUS);
		}

		if (maxHerdSize <= 0) {
			maxHerdSize = FRMC$DEFAULT_MAX_HERD_SIZE;
			this.frmc$setMaxHerdSize(maxHerdSize);
		}

		if (creatureCount >= maxHerdSize) {
			this.frmc$setOpenToCombine(false);
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void frmc$throttleCombineScan(CallbackInfo ci) {
		List<ComplexMob> creatureList = this.frmc$getCreatureList();
		if (creatureList == null) {
			this.frmc$setOpenToCombine(false);
			return;
		}

		int creatureCount = creatureList.size();
		int maxHerdSize = this.frmc$getMaxHerdSize();
		if (creatureCount == 0 || creatureCount >= maxHerdSize) {
			this.frmc$setOpenToCombine(false);
			return;
		}

		if (--this.frmc$combineScanTicker > 0) {
			this.frmc$setOpenToCombine(false);
			return;
		}

		this.frmc$combineScanTicker = FRMC$COMBINE_SCAN_STRIDE;
		this.frmc$setOpenToCombine(true);
	}

	@Unique
	private ComplexMob frmc$getLeader() {
		return (ComplexMob) this.frmc$getFieldValue("leader");
	}

	@Unique
	private void frmc$setLeader(ComplexMob leader) {
		this.frmc$setFieldValue("leader", leader);
	}


	@Unique
	private void frmc$setOpenToCombine(boolean openToCombine) {
		this.frmc$setFieldValue("openToCombine", openToCombine);
	}

	@Unique
	private float frmc$getRadius() {
		Object value = this.frmc$getFieldValue("radius");
		return value instanceof Number number ? number.floatValue() : FRMC$DEFAULT_RADIUS;
	}

	@Unique
	private void frmc$setRadius(float radius) {
		this.frmc$setFieldValue("radius", radius);
	}

	@Unique
	private int frmc$getMaxHerdSize() {
		Object value = this.frmc$getFieldValue("maxHerdSize");
		return value instanceof Number number ? number.intValue() : FRMC$DEFAULT_MAX_HERD_SIZE;
	}

	@Unique
	private void frmc$setMaxHerdSize(int maxHerdSize) {
		this.frmc$setFieldValue("maxHerdSize", maxHerdSize);
	}

	@Unique
	@SuppressWarnings("unchecked")
	private List<ComplexMob> frmc$getCreatureList() {
		Object value = this.frmc$getFieldValue("creatureList");
		return value instanceof List<?> list ? (List<ComplexMob>) list : new ArrayList<>();
	}

	@Unique
	private Object frmc$getFieldValue(String fieldName) {
		this.frmc$resolveFields();
		try {
			Field field = switch (fieldName) {
				case "leader" -> FRMC$LEADER_FIELD;
				case "openToCombine" -> FRMC$OPEN_TO_COMBINE_FIELD;
				case "radius" -> FRMC$RADIUS_FIELD;
				case "maxHerdSize" -> FRMC$MAX_HERD_SIZE_FIELD;
				case "creatureList" -> FRMC$CREATURE_LIST_FIELD;
				default -> null;
			};
			if (field == null) {
				return null;
			}
			return field.get(this);
		} catch (IllegalAccessException exception) {
			return null;
		}
	}

	@Unique
	private void frmc$setFieldValue(String fieldName, Object value) {
		this.frmc$resolveFields();
		try {
			Field field = switch (fieldName) {
				case "leader" -> FRMC$LEADER_FIELD;
				case "openToCombine" -> FRMC$OPEN_TO_COMBINE_FIELD;
				case "radius" -> FRMC$RADIUS_FIELD;
				case "maxHerdSize" -> FRMC$MAX_HERD_SIZE_FIELD;
				case "creatureList" -> FRMC$CREATURE_LIST_FIELD;
				default -> null;
			};
			if (field != null) {
				field.set(this, value);
			}
		} catch (IllegalAccessException exception) {
			// ...existing code...
		}
	}

	@Unique
	private void frmc$resolveFields() {
		if (FRMC$FIELDS_RESOLVED) {
			return;
		}

		synchronized (HerdEntityMixin.class) {
			if (FRMC$FIELDS_RESOLVED) {
				return;
			}

			try {
				Class<?> targetClass = this.getClass();
				FRMC$LEADER_FIELD = targetClass.getDeclaredField("leader");
				FRMC$OPEN_TO_COMBINE_FIELD = targetClass.getDeclaredField("openToCombine");
				FRMC$RADIUS_FIELD = targetClass.getDeclaredField("radius");
				FRMC$MAX_HERD_SIZE_FIELD = targetClass.getDeclaredField("maxHerdSize");
				FRMC$CREATURE_LIST_FIELD = targetClass.getDeclaredField("creatureList");
				FRMC$LEADER_FIELD.setAccessible(true);
				FRMC$OPEN_TO_COMBINE_FIELD.setAccessible(true);
				FRMC$RADIUS_FIELD.setAccessible(true);
				FRMC$MAX_HERD_SIZE_FIELD.setAccessible(true);
				FRMC$CREATURE_LIST_FIELD.setAccessible(true);
			} catch (ReflectiveOperationException exception) {
				FRMC$LEADER_FIELD = null;
				FRMC$OPEN_TO_COMBINE_FIELD = null;
				FRMC$RADIUS_FIELD = null;
				FRMC$MAX_HERD_SIZE_FIELD = null;
				FRMC$CREATURE_LIST_FIELD = null;
			}

			FRMC$FIELDS_RESOLVED = true;
		}
	}
}
