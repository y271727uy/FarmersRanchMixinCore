package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.client.render.entity.MagnetronRenderer;
import com.github.alexmodguy.alexscaves.client.render.entity.TeletorRenderer;
import com.y271727uy.FRMC.integration.alexscaves.MagneticLightningCache;
import java.util.Map;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = {MagnetronRenderer.class, TeletorRenderer.class}, remap = false)
public abstract class MagneticLightningRendererMixin implements MagneticLightningCache {
    @Shadow(remap = false) private Map<UUID, ?> lightningRenderMap;

    @Override
    public Map<UUID, ?> frmc$getLightningRenderMap() {
        return lightningRenderMap;
    }
}
