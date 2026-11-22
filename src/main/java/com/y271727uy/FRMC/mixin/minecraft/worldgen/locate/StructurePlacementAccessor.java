/*
 * Ported from BiomeSpy 1.3.3 for Minecraft 1.20.1.
 * Copyright (c) 2025 MoePus
 *
 * This file is licensed under the GNU Lesser General Public License, version 3 or later.
 * See third_party/BiomeSpy-NOTICE.txt and third_party/BiomeSpy-LICENSE.txt.
 *
 * Modified for FRMC: package names, mixin wiring and the removals listed in the NOTICE.
 */
package com.y271727uy.FRMC.mixin.minecraft.worldgen.locate;

import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.checkerframework.checker.units.qual.A;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(StructurePlacement.class)
public interface StructurePlacementAccessor {
    @Accessor
    StructurePlacement.FrequencyReductionMethod getFrequencyReductionMethod();
    @Accessor
    float getFrequency();
    @Accessor
    int getSalt();
    @Accessor
    Optional<StructurePlacement.ExclusionZone> getExclusionZone();
}
