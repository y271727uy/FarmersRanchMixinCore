package com.y271727uy.FRMC.mixin.nethers_exoticism;

import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismSupport;
import com.y271727uy.FRMC.integration.nethers_exoticism.NethersExoticismAdvancements;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.RamboutanBlockEntityCollidesInTheBlockProcedure", remap = false)
public abstract class RamboutanBlockEntityCollidesInTheBlockProcedureMixin {


    /**
     * @author y21727uy
     * @reason 操你妈的，滚犊子
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, Entity entity) {
        if (entity == null || !entity.isAlive() || entity instanceof ItemEntity) {
            return;
        }
        entity.hurt(NethersExoticismSupport.cactus(world), 1.0F);
        if (entity instanceof ServerPlayer player) {
            NethersExoticismAdvancements.award(player, NethersExoticismSupport.RAMBUTAN_ADVANCEMENT);
        }
    }
}
