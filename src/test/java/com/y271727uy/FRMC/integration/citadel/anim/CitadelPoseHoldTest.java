package com.y271727uy.FRMC.integration.citadel.anim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CitadelPoseHoldTest {
    /** 70° 垂直视野对应的投影矩阵 m11。 */
    private static final float FOV_70 = (float) (1.0 / Math.tan(Math.toRadians(35.0)));

    @Test
    void twoBlockMobIsSmallOnlyBeyondTheThresholdDistance() {
        // 1080p、70° 视野下，2 格高的实体在 d 格外约占 1542/d 像素；24 像素阈值对应约 64 格。
        assertFalse(CitadelPoseHold.isSmall(2.0, FOV_70, 1080, 60.0 * 60.0, 24));
        assertTrue(CitadelPoseHold.isSmall(2.0, FOV_70, 1080, 70.0 * 70.0, 24));
    }

    @Test
    void zoomingInMakesTheSameMobLarge() {
        float zoomed = FOV_70 * 4.0F;
        assertTrue(CitadelPoseHold.isSmall(2.0, FOV_70, 1080, 100.0 * 100.0, 24));
        assertFalse(CitadelPoseHold.isSmall(2.0, zoomed, 1080, 100.0 * 100.0, 24));
    }

    @Test
    void degenerateInputsNeverCountAsSmall() {
        assertFalse(CitadelPoseHold.isSmall(0.0, FOV_70, 1080, 100.0, 24));
        assertFalse(CitadelPoseHold.isSmall(2.0, FOV_70, 0, 100.0, 24));
        assertFalse(CitadelPoseHold.isSmall(2.0, FOV_70, 1080, 0.0, 24));
        assertFalse(CitadelPoseHold.isSmall(Double.NaN, FOV_70, 1080, 100.0, 24));
    }
}
