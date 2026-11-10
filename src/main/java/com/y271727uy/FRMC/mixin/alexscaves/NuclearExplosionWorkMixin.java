package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.entity.item.NuclearExplosionEntity;
import com.github.alexmodguy.alexscaves.server.misc.ACMath;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Stack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NuclearExplosionEntity.class)
public abstract class NuclearExplosionWorkMixin {
    @Shadow(remap = false) private Stack<BlockPos> destroyingChunks;
    @Shadow(remap = false) private boolean spawnedParticle;
    @Unique private boolean frmc$queuedDestruction;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", ordinal = 1, remap = false,
            target = "Ljava/util/Stack;isEmpty()Z"), require = 1)
    private boolean frmc$queueDestructionOnce(Stack<BlockPos> queue, Operation<Boolean> original) {
        if (frmc$queuedDestruction || !original.call(queue)) {
            return false;
        }
        frmc$queuedDestruction = true;
        return true;
    }

    @WrapOperation(method = "removeChunk", remap = false, at = @At(value = "INVOKE", remap = false, target =
            "Lcom/github/alexmodguy/alexscaves/server/misc/ACMath;sampleNoise3D(IIIF)F"), require = 1)
    private float frmc$skipNoiseOutsideCrater(int x, int y, int z, float radius, Operation<Float> original) {
        if (radius > 0.0F) {
            BlockPos center = ((Entity) (Object) this).blockPosition();
            float vertical = ACMath.smin(0.6F - Math.abs(center.getY() - y) / radius, 0.6F, 0.2F);
            // Four simplex corners contribute at most 32 * 4 * sqrt(2) * r * (0.6 - r²)^4.
            // This peaks below 4 at r² = 0.6 / 9. Negative vertical factors reverse the bound.
            float noiseBound = vertical < 0.0F ? -4.0F : 4.0F;
            float widthBound = (noiseBound - 0.5F) * 0.45F + 0.55F;
            double maxDistanceSquared = vertical * (radius + widthBound * radius) * radius;
            double dx = (double) x - center.getX();
            double dz = (double) z - center.getZ();
            if (dx * dx + 1.0D + dz * dz > maxDistanceSquared) {
                return 0.0F;
            }
        }
        return original.call(x, y, z, radius);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void frmc$saveExplosionProgress(CompoundTag tag, CallbackInfo ci) {
        var explosion = (NuclearExplosionEntity) (Object) this;
        tag.putFloat("FRMCExplosionSize", explosion.getSize());
        tag.putBoolean("FRMCNoGriefing", explosion.isNoGriefing());
        tag.putBoolean("FRMCIntentionalGameDesign", explosion.isIntentionalGameDesign());
        tag.putInt("FRMCExplosionAge", explosion.tickCount);
        tag.putBoolean("FRMCSpawnedParticle", spawnedParticle);
        tag.putBoolean("FRMCDestructionFinished", frmc$queuedDestruction && destroyingChunks.isEmpty());
        if (!destroyingChunks.isEmpty()) {
            int[] remaining = new int[destroyingChunks.size() * 3];
            int index = 0;
            for (BlockPos pos : destroyingChunks) {
                remaining[index++] = pos.getX();
                remaining[index++] = pos.getY();
                remaining[index++] = pos.getZ();
            }
            tag.putIntArray("FRMCRemainingDestruction", remaining);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void frmc$loadExplosionProgress(CompoundTag tag, CallbackInfo ci) {
        var explosion = (NuclearExplosionEntity) (Object) this;
        if (tag.contains("FRMCExplosionSize", Tag.TAG_ANY_NUMERIC)) {
            explosion.setSize(tag.getFloat("FRMCExplosionSize"));
            explosion.setNoGriefing(tag.getBoolean("FRMCNoGriefing"));
            explosion.setIntentionalGameDesign(tag.getBoolean("FRMCIntentionalGameDesign"));
            explosion.tickCount = tag.getInt("FRMCExplosionAge");
            spawnedParticle = tag.getBoolean("FRMCSpawnedParticle");
        }
        frmc$queuedDestruction = tag.getBoolean("FRMCDestructionFinished");
        destroyingChunks.clear();
        int[] remaining = tag.getIntArray("FRMCRemainingDestruction");
        if (!explosion.isNoGriefing() && remaining.length > 0 && remaining.length % 3 == 0) {
            for (int index = 0; index < remaining.length; index += 3) {
                destroyingChunks.push(new BlockPos(remaining[index], remaining[index + 1], remaining[index + 2]));
            }
            frmc$queuedDestruction = true;
        }
    }

    @Inject(method = "loadChunksAround", remap = false, at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$releaseExplosionTickets(boolean load, CallbackInfo ci) {
        var explosion = (NuclearExplosionEntity) (Object) this;
        if (load || !(explosion.level() instanceof ServerLevel level)) {
            return;
        }
        var data = level.getDataStorage().get(ForcedChunksSavedData::load, ForcedChunksSavedData.FILE_ID);
        if (data != null) {
            var owner = ForgeTicketOwnerInvoker.frmc$entityTicketOwner(AlexsCaves.MODID, explosion.getUUID());
            var ticking = data.getEntityForcedChunks().getTickingChunks().get(owner);
            if (ticking != null) {
                // Release the actual ticking tickets, even if the view distance changed.
                for (long pos : ticking.toLongArray()) {
                    var chunk = new ChunkPos(pos);
                    ForgeChunkManager.forceChunk(level, AlexsCaves.MODID, explosion, chunk.x, chunk.z, false, true);
                }
            }
        }
        ci.cancel();
    }
}
