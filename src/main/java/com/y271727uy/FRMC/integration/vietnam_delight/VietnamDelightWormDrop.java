package com.y271727uy.FRMC.integration.vietnam_delight;

import java.util.function.IntUnaryOperator;

/**
 * 原版 {@code WormDropHandler} 先在半径 16 格里按 4 格步进扫 81 次群系，再 {@code nextInt(4)}。
 * {@code Level.getBiome} 会把没加载的区块拉起来，下雨挖土时每格都走一遍。
 */
public final class VietnamDelightWormDrop {
    /** 下雨、靠近河流、锄头耕地：约 4% 掉 1 只。 */
    public static final int HOE_TILL_ONE_IN = 24;
    /** 下雨打碎泥土走的是同一套判定，再压一档，避免铲子比锄头更赚。 */
    public static final int BREAK_ONE_IN = 48;
    public static final int RIVER_RADIUS = 16;
    public static final int RIVER_STEP = 4;

    private VietnamDelightWormDrop() {
    }

    /** 未加载的列返回 null，调用方不得为此去加载区块。 */
    @FunctionalInterface
    public interface ColumnBiome {
        String pathAt(int blockX, int blockZ);
    }

    public static int roll(IntUnaryOperator nextInt, int oneIn) {
        if (oneIn <= 0) {
            return 0;
        }
        return nextInt.applyAsInt(oneIn) == 0 ? 1 : 0;
    }

    public static boolean containsRiver(String biomePath) {
        return biomePath != null && biomePath.contains("river");
    }

    /**
     * 与原版同一组采样点，但先看脚下。返回 true 就停，null 表示该列没加载，直接跳过。
     */
    public static boolean nearRiver(int blockX, int blockZ, ColumnBiome columns) {
        if (containsRiver(columns.pathAt(blockX, blockZ))) {
            return true;
        }
        for (int dx = -RIVER_RADIUS; dx <= RIVER_RADIUS; dx += RIVER_STEP) {
            for (int dz = -RIVER_RADIUS; dz <= RIVER_RADIUS; dz += RIVER_STEP) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (containsRiver(columns.pathAt(blockX + dx, blockZ + dz))) {
                    return true;
                }
            }
        }
        return false;
    }
}
