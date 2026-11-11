package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.client.particle.MagneticOrbitParticle;
import com.github.alexmodguy.alexscaves.client.particle.VentSmokeParticle;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Particle.class)
public abstract class CaveParticleCollisionMixin {
    @WrapOperation(method = "move", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"),
            require = 1)
    private Vec3 frmc$skipClearParticleCollision(Entity entity, Vec3 motion, AABB bounds, Level level,
            List<VoxelShape> hits, Operation<Vec3> original) {
        if (((Object) this instanceof VentSmokeParticle || (Object) this instanceof MagneticOrbitParticle)
                && entity == null && hits.isEmpty()
                && motion.lengthSqr() <= 1.0D && frmc$isClearParticleSweep(level, bounds.expandTowards(motion))) {
            return motion;
        }
        return original.call(entity, motion, bounds, level, hits);
    }

    @Unique
    private static boolean frmc$isClearParticleSweep(Level level, AABB sweep) {
        // Include the same outer block shell used by vanilla BlockCollisions.
        int minX = Mth.floor(sweep.minX - 1.0E-7D) - 1;
        int minY = Mth.floor(sweep.minY - 1.0E-7D) - 1;
        int minZ = Mth.floor(sweep.minZ - 1.0E-7D) - 1;
        int maxX = Mth.floor(sweep.maxX + 1.0E-7D) + 1;
        int maxY = Mth.floor(sweep.maxY + 1.0E-7D) + 1;
        int maxZ = Mth.floor(sweep.maxZ + 1.0E-7D) + 1;
        if ((minX >> 4) != (maxX >> 4) || (minY >> 4) != (maxY >> 4) || (minZ >> 4) != (maxZ >> 4)
                || level.isOutsideBuildHeight(minY) || level.isOutsideBuildHeight(maxY)
                || !(level.getChunkForCollisions(minX >> 4, minZ >> 4) instanceof LevelChunk chunk)) {
            return false;
        }
        var section = chunk.getSection(level.getSectionIndex(minY));
        if (!section.maybeHas(CaveParticleCollisionMixin::frmc$needsCollisionCheck)) {
            return true;
        }

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    int edges = (x == minX || x == maxX ? 1 : 0)
                            + (y == minY || y == maxY ? 1 : 0) + (z == minZ || z == maxZ ? 1 : 0);
                    if (edges == 3) {
                        continue;
                    }
                    BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                    if (edges == 0 ? frmc$needsCollisionCheck(state)
                            : edges == 1 ? state.hasLargeCollisionShape() : state.is(Blocks.MOVING_PISTON)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Unique
    private static boolean frmc$needsCollisionCheck(BlockState state) {
        return !state.is(Blocks.WATER) && !state.is(Blocks.AIR)
                && !state.is(Blocks.CAVE_AIR) && !state.is(Blocks.VOID_AIR);
    }
}
