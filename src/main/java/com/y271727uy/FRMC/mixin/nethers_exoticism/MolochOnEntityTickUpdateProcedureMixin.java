package com.y271727uy.FRMC.mixin.nethers_exoticism;

import net.mcreator.nethersexoticism.entity.MolochEntity;
import net.mcreator.nethersexoticism.init.NethersExoticismModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.mcreator.nethersexoticism.procedures.MolochOnEntityTickUpdateProcedure", remap = false)
public abstract class MolochOnEntityTickUpdateProcedureMixin {
    private static final String SKIN_KEY = "customSkin";
    private static final String RESOLVED_KEY = "frmcMolochSkinResolved";
    private static final String RESOLVED_NAME_KEY = "frmcMolochSkinName";

    /**
     * @author FRMC
     * @reason Keep egg and pet cooldowns, but stop rebuilding the custom-skin name chain on every tick.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (!(entity instanceof MolochEntity moloch) || !moloch.isAlive()) {
            return;
        }

        int petCooldown = moloch.getEntityData().get(MolochEntity.DATA_pet_cooldown);
        if (petCooldown > 0) {
            moloch.getEntityData().set(MolochEntity.DATA_pet_cooldown, petCooldown - 1);
        }

        if (!moloch.isBaby()) {
            int eggCooldown = moloch.getEntityData().get(MolochEntity.DATA_egg_cooldown);
            if (eggCooldown >= 12000) {
                moloch.getEntityData().set(MolochEntity.DATA_egg_cooldown, 0);
                if (world instanceof ServerLevel level) {
                    ItemEntity egg = new ItemEntity(level, x, y, z, new ItemStack(NethersExoticismModItems.MOLOCH_EGG.get()));
                    egg.setPickUpDelay(10);
                    level.addFreshEntity(egg);
                }
            } else {
                moloch.getEntityData().set(MolochEntity.DATA_egg_cooldown, eggCooldown + 1);
            }
        }

        updateSkin(moloch);
    }

    private static void updateSkin(MolochEntity moloch) {
        CompoundTag data = moloch.getPersistentData();
        String name = moloch.getName().getString();
        if (data.getBoolean(RESOLVED_KEY) && name.equals(data.getString(RESOLVED_NAME_KEY))) {
            return;
        }
        data.putBoolean(RESOLVED_KEY, true);
        data.putString(RESOLVED_NAME_KEY, name);
        data.putDouble(SKIN_KEY, skinFor(name));
    }

    private static double skinFor(String name) {
        return switch (name) {
            case "Moloch Gang" -> 1.0D;
            case "ChickenInKFC" -> 2.0D;
            case "Thatdanmgirll" -> 3.0D;
            case "DariusZeBaguette" -> 4.0D;
            case "Flannou" -> 5.0D;
            case "Leroidesafk" -> 6.0D;
            case "Skeleton" -> 7.0D;
            case "matteo" -> 8.0D;
            case "To0pa" -> 9.0D;
            case "WassMan" -> 10.0D;
            case "Yassou_" -> 11.0D;
            case "Zezer_123" -> 12.0D;
            default -> 0.0D;
        };
    }
}
