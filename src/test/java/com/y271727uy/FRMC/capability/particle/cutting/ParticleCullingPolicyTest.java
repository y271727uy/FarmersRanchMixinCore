package com.y271727uy.FRMC.capability.particle.cutting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ParticleCullingPolicyTest {
    @Test
    void usesNearestPointInsteadOfBoundsCenter() {
        AABB bounds = new AABB(10.0D, 0.0D, 0.0D, 20.0D, 1.0D, 1.0D);
        Vec3 camera = new Vec3(0.0D, 0.5D, 0.5D);

        assertTrue(ParticleCullingPolicy.withinDistance(bounds, camera, 100.0D));
        assertFalse(ParticleCullingPolicy.withinDistance(bounds, camera, 99.0D));
    }

    @Test
    void missingBoundsOrFrustumFailsOpen() {
        Vec3 camera = new Vec3(0.0D, 0.0D, 0.0D);
        assertFalse(ParticleCullingPolicy.shouldCull(null, camera, null, 1.0D, false));
        assertFalse(ParticleCullingPolicy.shouldCull(
                new AABB(100.0D, 100.0D, 100.0D, 101.0D, 101.0D, 101.0D),
                camera, null, 1.0D, true));
    }

    @Test
    void distanceRejectsOnlyWhenEntireBoundsAreOutside() {
        AABB bounds = new AABB(9.0D, -1.0D, -1.0D, 11.0D, 1.0D, 1.0D);
        assertFalse(ParticleCullingPolicy.shouldCull(
                bounds, new Vec3(0.0D, 0.0D, 0.0D), null, 100.0D, false));
        assertTrue(ParticleCullingPolicy.shouldCull(
                bounds, new Vec3(0.0D, 0.0D, 0.0D), null, 80.0D, false));
    }
}
