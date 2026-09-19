package com.y271727uy.FRMC.capability.betterfoliagecutting;

import java.util.function.IntPredicate;

/**
 * BetterFoliage fluff 十字挂在 NORTH/SOUTH 上，Chloride ALL 剔不掉。
 * 两刀：六邻都是叶则整块不画；北或南任一侧邻叶则丢掉整套十字（任意方向含 {@code dir == null}）。
 * Mixin 只在 Chloride 实际剔叶时调用这些判定；关剔除则 BF 原样。
 * <p>
 * 十字是一整片 ±45° 大面，不能按朝里/朝外拆。Embeddium 对不可见面会丢掉 {@code getQuads} 结果，
 * 南北都被叶夹住时那两面本来就不画，漏网是「一侧空气一侧叶」的外壳块：可见的那面会把整片十字伸进树心。
 */
public final class BetterFoliageInteriorCulling {
    public static final int DOWN = 0;
    public static final int UP = 1;
    public static final int NORTH = 2;
    public static final int SOUTH = 3;
    public static final int WEST = 4;
    public static final int EAST = 5;

    public static final int INTERIOR_MASK = 0b111111;
    public static final int NORTH_SOUTH_MASK = (1 << NORTH) | (1 << SOUTH);

    private BetterFoliageInteriorCulling() {
    }

    public static int collectMask(IntPredicate occludingToward) {
        int mask = 0;
        for (int direction3d = 0; direction3d < 6; direction3d++) {
            if (occludingToward.test(direction3d)) {
                mask |= 1 << direction3d;
            }
        }
        return mask;
    }

    public static boolean shouldCullEntirely(int mask) {
        return mask == INTERIOR_MASK;
    }

    public static boolean shouldDropCrosses(int mask) {
        return (mask & NORTH_SOUTH_MASK) != 0;
    }
}
