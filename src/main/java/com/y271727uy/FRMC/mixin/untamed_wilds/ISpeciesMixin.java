package com.y271727uy.FRMC.mixin.untamed_wilds;

import java.util.Locale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import untamedwilds.entity.ComplexMob;

@Pseudo
@Mixin(targets = "untamedwilds.entity.ISpecies", remap = false)
public interface ISpeciesMixin {
    /**
     * @author FRMC
     * @reason The original interface default method directly indexes speciesData without bounds checks.
     */
    @Overwrite
    default String getRawSpeciesName(int species) {
        ComplexMob mob = (ComplexMob) (Object) this;
        return ComplexMob.getEntityData(mob.getType()).getName(species).toLowerCase(Locale.ROOT);
    }
}
