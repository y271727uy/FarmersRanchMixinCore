package com.y271727uy.FRMC.integration.computatio;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DirectNavigationPath extends Path {
    private final AABB checkedArea;

    public DirectNavigationPath(Mob mob, BlockPos target, BlockPathTypes type) {
        super(ObjectArrayList.of(node(target, type)), target, true);
        // Include the floor and neighboring cells used by the traversal checks.
        checkedArea = mob.getBoundingBox().expandTowards(nodePosition(mob, target).subtract(mob.position()))
                .inflate(1.0D);
    }

    public static Vec3 nodePosition(Mob mob, BlockPos node) {
        // Match Path.getEntityPosAtNode, including its offset for wider mobs.
        double offset = (int) (mob.getBbWidth() + 1.0F) * 0.5D;
        return new Vec3(node.getX() + offset, node.getY(), node.getZ() + offset);
    }

    public boolean crosses(BlockPos pos) {
        return checkedArea.intersects(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
    }

    private static Node node(BlockPos pos, BlockPathTypes type) {
        Node node = new Node(pos.getX(), pos.getY(), pos.getZ());
        node.type = type;
        return node;
    }
}
