package com.y271727uy.FRMC.mixin.manors_bounty;

import net.mcreator.manors_bounty.init.ManorsBountyModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(targets = "net.mcreator.manors_bounty.procedures.HappyAuraTickProcedure")
public abstract class HappyAuraTickProcedureMixin {
    @Unique
    private static final double FRMC$MIN_AMPLIFIER = 2.0D;
    @Unique
    private static final double FRMC$BASE_AURA_RANGE = 6.0D;
    @Unique
    private static final double FRMC$BASE_BOOST_RANGE = 12.0D;
    @Unique
    private static final int FRMC$AURA_DURATION = 100;
    @Unique
    private static final int FRMC$BOOST_DURATION = 30;

    /**
     * @author FRMC
     * @reason Replace repeated entity scans and sorting with a single compatibility-focused player scan.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, double amplifier) {
        if (!(entity instanceof LivingEntity livingEntity) || world.isClientSide() || amplifier < FRMC$MIN_AMPLIFIER) {
            return;
        }

        Vec3 center = new Vec3(x, y, z);
        double auraRadius = frmc$getRadius(FRMC$BASE_AURA_RANGE, amplifier);
        double boostRadius = frmc$getRadius(FRMC$BASE_BOOST_RANGE, amplifier);
        double auraRadiusSqr = auraRadius * auraRadius;
        double boostRadiusSqr = boostRadius * boostRadius;

        List<Player> nearbyPlayers = world.getEntitiesOfClass(Player.class, new AABB(center, center).inflate(boostRadius), player -> player != entity);
        if (nearbyPlayers.isEmpty()) {
            return;
        }

        boolean shouldApplyAura = false;
        boolean shouldApplyBoost = false;
        for (Player nearbyPlayer : nearbyPlayers) {
            double distanceSqr = nearbyPlayer.distanceToSqr(center);
            if (!shouldApplyAura && distanceSqr <= auraRadiusSqr) {
                shouldApplyAura = true;
            }

            if (!shouldApplyBoost && distanceSqr <= boostRadiusSqr && frmc$hasMatchingAura(nearbyPlayer, amplifier)) {
                shouldApplyBoost = true;
            }

            if (shouldApplyAura && shouldApplyBoost) {
                break;
            }
        }

        if (shouldApplyAura) {
            livingEntity.addEffect(new MobEffectInstance(ManorsBountyModMobEffects.HAPPY_AURA.get(), FRMC$AURA_DURATION, (int) (amplifier - 2.0D), false, true));
        }

        if (shouldApplyBoost) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, FRMC$BOOST_DURATION, (int) (((amplifier + 1.0D) * 0.5D) - 2.0D), false, true));
        }
    }

    @Unique
    private static double frmc$getRadius(double baseRange, double amplifier) {
        return (baseRange + (amplifier + 1.0D) * 0.5D) * 0.5D;
    }

    @Unique
    private static boolean frmc$hasMatchingAura(LivingEntity entity, double amplifier) {
        MobEffectInstance effect = entity.getEffect(ManorsBountyModMobEffects.HAPPY_AURA.get());
        return effect != null && effect.getAmplifier() == (int) amplifier;
    }
}

