package com.y271727uy.FRMC.integration.alexscaves;

import java.util.function.Supplier;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class HullbreakerRenderMaterials {
    private static final ResourceLocation BODY = ResourceLocation.fromNamespaceAndPath("alexscaves", "textures/entity/hullbreaker.png");
    private static RenderType glow;

    private HullbreakerRenderMaterials() {
    }

    public static RenderType glow(Supplier<RenderType> factory) {
        if (glow == null) {
            glow = factory.get();
        }
        return glow;
    }

    public static boolean isBodyOrGlow(RenderType material) {
        return material == glow || material == RenderType.entityTranslucent(BODY);
    }
}
