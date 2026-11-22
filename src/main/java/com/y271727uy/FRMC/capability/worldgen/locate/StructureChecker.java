/*
 * Ported from BiomeSpy 1.3.3 for Minecraft 1.20.1.
 * Copyright (c) 2025 MoePus
 *
 * This file is licensed under the GNU Lesser General Public License, version 3 or later.
 * See third_party/BiomeSpy-NOTICE.txt and third_party/BiomeSpy-LICENSE.txt.
 *
 * Modified for FRMC: package names, mixin wiring and the removals listed in the NOTICE.
 */
package com.y271727uy.FRMC.capability.worldgen.locate;

import com.y271727uy.FRMC.capability.worldgen.locate.BiomeEnvelopeSelector;
import com.y271727uy.FRMC.capability.worldgen.locate.LazyBiomeNoiseChecker;
import com.y271727uy.FRMC.integration.alexscaves.locate.AlexBiome;
import com.y271727uy.FRMC.integration.alexscaves.locate.AlexsCavesCompat;
import com.y271727uy.FRMC.mixin.minecraft.worldgen.locate.StructureCheckAccessor;
import com.y271727uy.FRMC.mixin.minecraft.worldgen.locate.StructureManagerAccessor;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.Map;

/**
 * FRMC: BiomeSpy's {@code biomespy:uninit_safe} shortcut was removed. It reported buried treasure, igloos and
 * ruined portals without loading the chunk to confirm a valid start, so /locate could point at a structure that
 * does not exist. Every candidate that passes the biome prefilter is now confirmed the vanilla way.
 */
public class StructureChecker {
    private static boolean tryAddReference(StructureManager pStructureManager, StructureStart pStructureStart) {
        if (pStructureStart.canBeReferenced()) {
            pStructureManager.addReference(pStructureStart);
            return true;
        }
        return false;
    }

    private static StructureCheckResult checkStructureInfo(Object2IntMap<Structure> pStructureChunks, Structure pStructure, boolean pSkipKnownStructures) {
        int i = pStructureChunks.getOrDefault(pStructure, -1);
        return i == -1 || pSkipKnownStructures && i != 0 ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.START_PRESENT;
    }

    public static Pair<BlockPos, Holder<Structure>>
    getStructureGeneratingAt(Map<Holder<Structure>, BiomeEnvelopeSelector> pStructureHoldersSet, LevelReader pLevel,
                             StructureManager pStructureManager, boolean pSkipKnownStructures, StructurePlacement pPlacement,
                             ChunkPos pChunkPos, Climate.ParameterList<Holder<Biome>> parameters) {
        StructureCheckAccessor structureCheckAccessor = (StructureCheckAccessor) (((StructureManagerAccessor) pStructureManager).getStructureCheck());

        Climate.Sampler sampler = structureCheckAccessor.getRandomState().sampler();
        Object2IntMap<Structure> structureChunkMap = structureCheckAccessor.getLoadedChunks().get(pChunkPos.toLong());
        int x = pChunkPos.getMinBlockX();
        int z = pChunkPos.getMinBlockZ();

        for (var entry : pStructureHoldersSet.entrySet()) {
            Holder<Structure> holder = entry.getKey();

            StructureCheckResult structurecheckresult;
            if (structureChunkMap != null) {
                structurecheckresult = checkStructureInfo(structureChunkMap, holder.value(), pSkipKnownStructures);
            } else {
                if (!AlexsCavesCompat.ALEXS_CAVES_INSTALLED || !AlexBiome.isAlexsCavesChunk(entry.getValue(), x, z)) {
                    LazyBiomeNoiseChecker biomeChecker = new LazyBiomeNoiseChecker(entry.getValue(), parameters, x, z);
                    if (!biomeChecker.matches(sampler))
                        continue;
                }

                structurecheckresult = pStructureManager.checkStructurePresence(pChunkPos, holder.value(), pSkipKnownStructures);
            }
            if (structurecheckresult == StructureCheckResult.START_NOT_PRESENT) continue;

            if (!pSkipKnownStructures) {
                if (structurecheckresult == StructureCheckResult.START_PRESENT) {
                    return Pair.of(pPlacement.getLocatePos(pChunkPos), holder);
                }
            }

            // SkipKnownStructures || StructureCheckResult.CHUNK_LOAD_NEEDED
            ChunkAccess chunkaccess = pLevel.getChunk(pChunkPos.x, pChunkPos.z, ChunkStatus.STRUCTURE_STARTS);
            StructureStart structurestart = pStructureManager.getStartForStructure(SectionPos.bottomOf(chunkaccess), holder.value(), chunkaccess);
            if (structurestart != null && structurestart.isValid() && (!pSkipKnownStructures || tryAddReference(pStructureManager, structurestart))) {
                return Pair.of(pPlacement.getLocatePos(structurestart.getChunkPos()), holder);
            }
        }

        return null;
    }
}
