package com.y271727uy.FRMC.capability.villager.optimization;

/**
 * Detects a path that cannot reach its target and also makes no horizontal progress: it ends in the column the
 * villager already stands in. Vanilla MoveToTargetSink starts such a path, navigation finishes it at once, and
 * because the villager is "done" rather than "stuck" no cooldown applies, so the same unreachable target is
 * searched again on the next tick, exhausting the node budget each time.
 *
 * <p>The vertical tolerance of one block absorbs start-node height differences (slabs, dirt paths, farmland)
 * between the entity's block position and the path's end node. Villagers cannot climb, so a path that never leaves
 * the column cannot move them anywhere.
 */
public final class ZeroProgressPath {
    public static final int RETRY_COOLDOWN_BOUND = 40;

    private ZeroProgressPath() {
    }

    public static boolean isZeroProgress(boolean canReach, int endX, int endY, int endZ, int x, int y, int z) {
        return !canReach && endX == x && endZ == z && Math.abs(endY - y) <= 1;
    }
}
