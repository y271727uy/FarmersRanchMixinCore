package com.y271727uy.FRMC.mixin.nethers_exoticism;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.WitherCureOnPotionActiveTickProcedure", remap = false)
public abstract class WitherCureOnPotionActiveTickProcedureMixin {
    /**
     * @author FRMC
     * @reason Remove wither only when it is actually present.
     */
    @Overwrite(remap = false)
    public static void execute(Entity entity) {
        if (entity instanceof LivingEntity living && living.hasEffect(MobEffects.WITHER)) {
            living.removeEffect(MobEffects.WITHER);
        }
    }
}
