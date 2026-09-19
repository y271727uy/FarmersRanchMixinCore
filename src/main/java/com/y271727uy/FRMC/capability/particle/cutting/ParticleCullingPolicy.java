package com.y271727uy.FRMC.capability.particle.cutting;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Stateless client-side particle visibility checks.
 *
 * <p>This class deliberately owns only conservative geometric culling. It does
 * not decide which particle types are disabled and it does not perform block
 * ray casts or GPU queries.</p>
 */
public final class ParticleCullingPolicy {
    private ParticleCullingPolicy() {
    }

    /** Returns whether the particle bounds are within the configured radius. */
    public static boolean withinDistance(AABB bounds, Vec3 camera, double maxDistanceSqr) {
        if (bounds == null || camera == null || maxDistanceSqr < 0.0D) {
            return true;
        }

        double x = clamp(camera.x, bounds.minX, bounds.maxX);
        double y = clamp(camera.y, bounds.minY, bounds.maxY);
        double z = clamp(camera.z, bounds.minZ, bounds.maxZ);
        double dx = camera.x - x;
        double dy = camera.y - y;
        double dz = camera.z - z;
        return dx * dx + dy * dy + dz * dz <= maxDistanceSqr;
    }

    /** Returns false only when a non-null frustum proves the bounds are outside. */
    public static boolean withinFrustum(AABB bounds, Frustum frustum) {
        return bounds == null || frustum == null || frustum.isVisible(bounds);
    }

    /**
     * Returns true when a particle can be discarded before rendering.
     * Missing render state fails open so compatibility integrations cannot make
     * particles disappear merely because a renderer is not ready yet.
     */
    public static boolean shouldCull(AABB bounds, Vec3 camera, Frustum frustum,
                                     double maxDistanceSqr, boolean alwaysRender) {
        if (alwaysRender || bounds == null) {
            return false;
        }
        return !withinDistance(bounds, camera, maxDistanceSqr)
                || !withinFrustum(bounds, frustum);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
