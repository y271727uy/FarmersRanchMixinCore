package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.living.LuxtructosaurusEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LuxtructosaurusEntity.class)
public abstract class LuxtructosaurusRoarScanMixin {
    @Unique private static final int FRMC_ROAR_SCAN_INTERVAL = 4;

    @Unique private Level frmc$roarLevel;
    @Unique private AABB frmc$roarBounds;
    @Unique private List<LivingEntity> frmc$roarCandidates;
    @Unique private int frmc$roarScanTick;
    @Unique private int frmc$lastRoarTick;

    @WrapOperation(method = "scareMobs", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"),
            require = 1)
    private List<LivingEntity> frmc$reuseRoarCandidates(Level level, Class<LivingEntity> type, AABB bounds,
            Operation<List<LivingEntity>> original) {
        int tick = ((LuxtructosaurusEntity) (Object) this).tickCount;
        frmc$lastRoarTick = tick;
        if (frmc$roarCandidates == null || level != frmc$roarLevel || !bounds.equals(frmc$roarBounds)
                || tick - frmc$roarScanTick >= FRMC_ROAR_SCAN_INTERVAL) {
            List<LivingEntity> candidates = original.call(level, type, bounds);
            frmc$roarLevel = level;
            frmc$roarBounds = bounds;
            frmc$roarScanTick = tick;
            frmc$roarCandidates = candidates;
            return candidates;
        }

        List<LivingEntity> stillInRange = new ArrayList<>();
        for (LivingEntity candidate : frmc$roarCandidates) {
            if (!candidate.isRemoved() && candidate.level() == level && EntitySelector.NO_SPECTATORS.test(candidate)
                    && bounds.intersects(candidate.getBoundingBox())) {
                stillInRange.add(candidate);
            }
        }
        return stillInRange;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void frmc$clearEndedRoar(CallbackInfo ci) {
        if (frmc$roarCandidates != null && frmc$lastRoarTick != ((LuxtructosaurusEntity) (Object) this).tickCount) {
            frmc$roarCandidates = null;
            frmc$roarLevel = null;
            frmc$roarBounds = null;
        }
    }
}
