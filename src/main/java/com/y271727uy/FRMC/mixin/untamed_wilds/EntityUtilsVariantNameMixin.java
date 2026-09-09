package com.y271727uy.FRMC.mixin.untamed_wilds;

import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import untamedwilds.entity.ComplexMob;
import untamedwilds.util.EntityDataHolder;
import untamedwilds.util.EntityDataHolderClient;
import untamedwilds.util.EntityUtils;

@Pseudo
@Mixin(value = EntityUtils.class, remap = false)
public abstract class EntityUtilsVariantNameMixin {
    /**
     * @author FRMC
     * @reason Item NBT can contain a species index outside the loaded entity data range.
     */
    @Overwrite
    public static String getVariantName(EntityType<?> entityType, int species) {
        EntityDataHolder entityData = ComplexMob.ENTITY_DATA_HASH.get(entityType);
        if (entityData != null) {
            if (species < 0 || species >= entityData.getSpeciesData().size()) {
                return "";
            }
            return entityData.getName(species);
        }

        EntityDataHolderClient clientData = ComplexMob.CLIENT_DATA_HASH.get(entityType);
        return clientData != null ? clientData.getSpeciesName(species) : "";
    }
}
