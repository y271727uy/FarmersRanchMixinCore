package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.effect.ArcEffect;
import com.cloudmeow.delightoflight.entity.ElectricCurrentEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = ArcEffect.class, remap = false)
public abstract class ArcEffectMixin {
    // Keep only IDs so cached projectiles cannot keep their owners or levels alive.
    @Unique
    private static final Map<LivingEntity, Integer> FRMC$KNOWN_ARCS =
            Collections.synchronizedMap(new WeakHashMap<>());

    @WrapOperation(method = {"applyEffectTick(Lnet/minecraft/world/entity/LivingEntity;I)V",
            "m_6742_(Lnet/minecraft/world/entity/LivingEntity;I)V"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
                    remap = true), require = 1)
    private List<ElectricCurrentEntity> frmc$findOwnedArc(Level level, Class<ElectricCurrentEntity> type,
                                                         AABB bounds, Operation<List<ElectricCurrentEntity>> original, LivingEntity owner, int amplifier) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return original.call(level, type, bounds);
        }

        Integer id = FRMC$KNOWN_ARCS.get(owner);
        if (id != null && serverLevel.getEntity(id) instanceof ElectricCurrentEntity arc
                && arc.level() == level && !arc.isRemoved() && !arc.isSpectator()
                && arc.getBoundingBox().intersects(bounds) && frmc$isOwnedBy(arc, owner)) {
            return List.of(arc);
        }

        List<ElectricCurrentEntity> arcs = original.call(level, type, bounds);
        for (ElectricCurrentEntity arc : arcs) {
            if (frmc$isOwnedBy(arc, owner)) {
                FRMC$KNOWN_ARCS.put(owner, arc.getId());
                return arcs;
            }
        }
        if (id != null) {
            FRMC$KNOWN_ARCS.remove(owner);
        }
        return arcs;
    }

    @Unique
    private static boolean frmc$isOwnedBy(ElectricCurrentEntity arc, LivingEntity owner) {
        Entity currentOwner = arc.getOwner();
        return currentOwner != null && currentOwner.getUUID().equals(owner.getUUID());
    }
}
