package com.y271727uy.FRMC.mixin.untamed_wilds;

import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import untamedwilds.entity.ComplexMob;
import untamedwilds.util.EntityDataHolderClient;
import untamedwilds.util.EntityUtils;

@Pseudo
@Mixin(value = EntityUtils.class, remap = false)
public abstract class EntityUtilsSpeciesCountMixin {
    /**
     * @author FRMC
     * @reason Client texture sync owns the complete species list after login, even while the server-only loaded flag is false.
     */
    @Overwrite
    public static int getNumberOfSpecies(EntityType<?> entityType) {
        EntityDataHolderClient clientData = ComplexMob.CLIENT_DATA_HASH.get(entityType);
        if (clientData != null) {
            int speciesCount = clientData.getNumberOfSpecies();
            if (speciesCount > 0) {
                return speciesCount;
            }
        }

        return 99;
    }
}
