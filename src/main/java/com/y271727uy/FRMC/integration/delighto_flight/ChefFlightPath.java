package com.y271727uy.FRMC.integration.delighto_flight;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

public final class ChefFlightPath extends Path {
    private final int arrivalDistance;
    private final AABB watchedArea;

    ChefFlightPath(List<Node> nodes, BlockPos target, int arrivalDistance, AABB corridor) {
        super(nodes, target, true);
        this.arrivalDistance = arrivalDistance;
        // Collision shapes from nearby blocks can extend into the corridor.
        this.watchedArea = corridor.inflate(1.0);
    }

    public boolean crosses(BlockPos changedBlock) {
        return watchedArea.intersects(new AABB(changedBlock));
    }

    public boolean hasArrived(BlockPos position) {
        return getTarget().distManhattan(position) <= arrivalDistance;
    }
}
