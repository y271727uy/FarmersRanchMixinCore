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

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureCheck.class)
public interface StructureCheckAccessor {
    @Accessor
    ChunkScanAccess getStorageAccess();

    @Accessor
    RegistryAccess getRegistryAccess();

    @Accessor
    StructureTemplateManager getStructureTemplateManager();

    @Accessor
    ResourceKey<Level> getDimension();

    @Accessor
    ChunkGenerator getChunkGenerator();

    @Accessor
    RandomState getRandomState();

    @Accessor
    long getSeed();

    @Accessor
    LevelHeightAccessor getHeightAccessor();

    @Accessor
    BiomeSource getBiomeSource();

    @Accessor
    Long2ObjectMap<Object2IntMap<Structure>> getLoadedChunks();
}
