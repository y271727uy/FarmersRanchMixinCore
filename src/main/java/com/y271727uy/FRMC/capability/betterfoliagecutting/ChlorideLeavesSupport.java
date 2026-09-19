package com.y271727uy.FRMC.capability.betterfoliagecutting;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraftforge.fml.ModList;

/**
 * Leaf-or-not for interior culling, plus whether Chloride is actually skipping leaf faces.
 * Chloride types stay in inner classes so missing Chloride cannot NCDFE.
 */
public final class ChlorideLeavesSupport {
    private static final boolean CHLORIDE_PRESENT = detectChloride();

    private ChlorideLeavesSupport() {
    }

    public static boolean isOccludingLeaf(Block block) {
        if (block instanceof LeavesBlock) {
            return true;
        }
        return CHLORIDE_PRESENT && ChlorideGameLeaves.isGameLeaves(block);
    }

    /**
     * True when Chloride would skip a matching neighbor leaf face.
     * {@code LeavesCulling.should()} is true for Embeddium Fast leaves even if the
     * Chloride option is OFF, so Fast is counted here too. Missing Chloride is false.
     */
    public static boolean isLeavesCullingActive() {
        return CHLORIDE_PRESENT && ChlorideLeavesCulling.isActive();
    }

    private static boolean detectChloride() {
        try {
            return ModList.get().isLoaded("chloride");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class ChlorideGameLeaves {
        private ChlorideGameLeaves() {
        }

        static boolean isGameLeaves(Block block) {
            return block instanceof me.srrapero720.chloride.api.IGameLeaves;
        }
    }

    private static final class ChlorideLeavesCulling {
        private ChlorideLeavesCulling() {
        }

        static boolean isActive() {
            try {
                if (me.srrapero720.chloride.impl.LeavesCulling.fastLeaves()) {
                    return true;
                }
                return me.srrapero720.chloride.ChlorideConfig.world.leavesCulling
                    == me.srrapero720.chloride.impl.LeavesCulling.LeavesCullingMode.ALL;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }
}
