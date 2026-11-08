package com.y271727uy.FRMC.integration.computatio;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;

public interface CitadelGroundPathJob {
    void frmc$capture(Mob mob, BlockPos start, BlockPos target, int range);

    CitadelGroundPath frmc$takeCandidate();
}
