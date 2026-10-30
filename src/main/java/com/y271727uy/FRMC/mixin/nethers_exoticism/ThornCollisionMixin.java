package com.y271727uy.FRMC.mixin.nethers_exoticism;

import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismSupport;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = {
    "net.mcreator.nethersexoticism.procedures.PitayaStemEntityCollidesInTheBlockProcedure",
    "net.mcreator.nethersexoticism.procedures.PitayaSaplingMobplayerCollidesWithPlantProcedure"
}, remap = false)
public abstract class ThornCollisionMixin {
    /**
     * @author FRMC
     * @reason Apply cactus damage without rebuilding the damage lookup path for every colliding entity.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, Entity entity) {
        if (entity == null || !entity.isAlive()) {
            return;
        }
        entity.hurt(NethersExoticismSupport.cactus(world), 1.0F);
    }
}
