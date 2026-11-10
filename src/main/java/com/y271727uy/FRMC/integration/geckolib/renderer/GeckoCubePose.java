package com.y271727uy.FRMC.integration.geckolib.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.GeoCube;

public final class GeckoCubePose {
    private GeckoCubePose() {}

    public static void rotate(PoseStack poseStack, GeoCube cube) {
        Vec3 rotation = cube.rotation();
        if (!rotation.equals(Vec3.ZERO)) {
            Vec3 pivot = cube.pivot();
            poseStack.rotateAround(new Quaternionf().rotationZYX((float) rotation.z, (float) rotation.y,
                    (float) rotation.x), (float) (pivot.x() / 16.0F), (float) (pivot.y() / 16.0F),
                    (float) (pivot.z() / 16.0F));
        }
    }
}
