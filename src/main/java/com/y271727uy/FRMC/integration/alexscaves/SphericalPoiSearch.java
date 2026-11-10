package com.y271727uy.FRMC.integration.alexscaves;

import com.y271727uy.FRMC.mixin.alexscaves.PoiSectionStorageAccessor;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;

public final class SphericalPoiSearch {
    private SphericalPoiSearch() {
    }

    public static boolean canSearch(ServerLevel level) {
        return level.getServer().isSameThread() && level.getPoiManager() instanceof PoiSectionStorageAccessor<?>;
    }

    /** Visits matching POIs in vanilla encounter order, stopping when the visitor returns true. */
    @SuppressWarnings("unchecked")
    public static BlockPos visit(ServerLevel level, BlockPos center, int range, ResourceKey<PoiType> poi,
            Predicate<BlockPos> visitor) {
        var storage = (PoiSectionStorageAccessor<PoiSection>) level.getPoiManager();
        int radiusSquared = range * range;
        try (var chunks = ChunkPos.rangeClosed(new ChunkPos(center), Math.floorDiv(range, 16) + 1)) {
            for (var iterator = chunks.iterator(); iterator.hasNext();) {
                var chunk = iterator.next();
                int dx = Math.max(0, Math.max(chunk.getMinBlockX() - center.getX(), center.getX() - chunk.getMaxBlockX()));
                int dz = Math.max(0, Math.max(chunk.getMinBlockZ() - center.getZ(), center.getZ() - chunk.getMaxBlockZ()));
                int verticalSquared = radiusSquared - dx * dx - dz * dz;
                if (verticalSquared < 0) {
                    continue;
                }
                int height = (int) Math.sqrt(verticalSquared);
                int minSection = Math.max(level.getMinSection(), SectionPos.blockToSectionCoord(center.getY() - height));
                int maxSection = Math.min(level.getMaxSection() - 1, SectionPos.blockToSectionCoord(center.getY() + height));
                for (int sectionY = minSection; sectionY <= maxSection; sectionY++) {
                    var section = storage.frmc$getOrLoadPoiSection(SectionPos.asLong(chunk.x, sectionY, chunk.z));
                    if (section.isEmpty()) {
                        continue;
                    }
                    var match = section.get().getRecords(type -> type.is(poi), PoiManager.Occupancy.ANY)
                            .map(record -> record.getPos())
                            .filter(pos -> pos.distSqr(center) <= radiusSquared && visitor.test(pos))
                            .findFirst();
                    if (match.isPresent()) {
                        return match.get();
                    }
                }
            }
        }
        return null;
    }
}
