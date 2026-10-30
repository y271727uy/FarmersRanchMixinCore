package com.y271727uy.FRMC.mixin.nethers_exoticism;

import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismSupport;
import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismAdvancements;

import net.mcreator.nethersexoticism.entity.RambutanThornProjectileEntity;
import net.mcreator.nethersexoticism.init.NethersExoticismModEntities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.RamboutansHelmetTickEventProcedure", remap = false)
public abstract class RamboutansHelmetTickEventProcedureMixin {
    private static final String TOGGLE_KEY = "playerToggle";
    private static final String PITCH_KEY = "playerThornPitch";
    private static final String YAW_KEY = "playerThornYaw";
    private static final double SHAKE_THRESHOLD = 20.0D;

    /**
     * @author FRMC
     * @reason Keep the helmet's crouch damage and head-shake thorn, without scanning spectators or rebuilding sounds every tick.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (!(entity instanceof Player player) || !player.isAlive() || player.isSpectator()) {
            return;
        }
        if (player.isShiftKeyDown()) {
            damageNearby(world, player);
            return;
        }
        if (!(world instanceof Level level) || level.isClientSide()) {
            return;
        }

        var data = player.getPersistentData();
        if (!data.getBoolean(TOGGLE_KEY)) {
            data.putDouble(PITCH_KEY, player.getXRot());
            data.putDouble(YAW_KEY, player.getYRot());
            data.putBoolean(TOGGLE_KEY, true);
            return;
        }
        data.putBoolean(TOGGLE_KEY, false);

        boolean shaken = Math.abs(player.getYRot() - data.getDouble(YAW_KEY)) > SHAKE_THRESHOLD
            || Math.abs(player.getXRot() - data.getDouble(PITCH_KEY)) > SHAKE_THRESHOLD;
        if (!shaken) {
            return;
        }

        NethersExoticismSupport.play(level, player.blockPosition(), NethersExoticismSupport.SWEET_BERRY_BREAK, SoundSource.NEUTRAL, 0.2F, 2.0F);
        if (player instanceof ServerPlayer serverPlayer) {
            NethersExoticismAdvancements.award(serverPlayer, NethersExoticismSupport.RAMBUTAN_SHAKE_ADVANCEMENT);
        }
        RambutanThornProjectileEntity projectile = new RambutanThornProjectileEntity(NethersExoticismModEntities.RAMBUTAN_THORN_PROJECTILE.get(), level);
        projectile.setOwner(player);
        projectile.setBaseDamage(2.0D);
        projectile.setKnockback(0);
        projectile.setSilent(true);
        projectile.setPierceLevel((byte) 1);
        projectile.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        projectile.shoot(player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z, 0.8F, 1.5F);
        level.addFreshEntity(projectile);
    }

    private static void damageNearby(LevelAccessor world, Player player) {
        AABB box = player.getBoundingBox().inflate(0.5D);
        for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player && entity.isAlive())) {
            target.hurt(NethersExoticismSupport.cactus(world), 1.0F);
            if (player instanceof ServerPlayer serverPlayer) {
                NethersExoticismAdvancements.award(serverPlayer, NethersExoticismSupport.RAMBUTAN_HELMET_ADVANCEMENT);
            }
        }
    }
}
