package com.y271727uy.FRMC.mixin.citadel.pathfinding;

import com.github.alexthe666.citadel.server.entity.pathfinding.raycoms.PathResult;
import com.github.alexthe666.citadel.server.entity.pathfinding.raycoms.pathjobs.PathJobMoveToLocation;
import com.y271727uy.FRMC.integration.computatio.CitadelGroundPath;
import com.y271727uy.FRMC.integration.computatio.CitadelGroundPathJob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PathJobMoveToLocation.class, remap = false)
public abstract class PathJobMoveToLocationMixin implements CitadelGroundPathJob {
    @Unique private CitadelGroundPath frmc$candidate;

    @Override
    public void frmc$capture(Mob mob, BlockPos start, BlockPos target, int range) {
        frmc$candidate = CitadelGroundPath.capture(mob, start, target, range);
    }

    @Override
    public CitadelGroundPath frmc$takeCandidate() {
        CitadelGroundPath candidate = frmc$candidate;
        frmc$candidate = null;
        return candidate;
    }

    @Inject(method = "search", at = @At("HEAD"), cancellable = true)
    private void frmc$searchGroundJob(CallbackInfoReturnable<Path> cir) {
        if (frmc$candidate == null) {
            return;
        }
        Path path = frmc$candidate.find();
        if (path == null) {
            frmc$candidate = null;
            return;
        }
        PathResult<?> result = ((PathJobMoveToLocation) (Object) this).getResult();
        result.setPathReachesDestination(true);
        cir.setReturnValue(path);
    }
}
