package com.y271727uy.FRMC.integration.delighto_flight;

import com.cloudmeow.delightoflight.registry.DFEffects;
import com.cloudmeow.delightoflight.utility.DFUtilities;
import com.y271727uy.FRMC.FRMCMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FRMCMod.MODID)
public final class ProjectileDodgeEvents {
    private ProjectileDodgeEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile instanceof ProjectileDodgeState state) || projectile.level().isClientSide
                || event.getImpactResult() != ProjectileImpactEvent.ImpactResult.DEFAULT
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        MobEffectInstance dim = target.getEffect(DFEffects.DIM.get());
        if (dim == null || (!projectile.level().isNight() && !DFUtilities.hasGrindHerbs(target))) {
            return;
        }
        if (projectile.level().getRandom().nextFloat() < 0.1F * (dim.getAmplifier() + 1)) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
        } else {
            state.frmc$markDodgeChecked(hit);
        }
    }
}
