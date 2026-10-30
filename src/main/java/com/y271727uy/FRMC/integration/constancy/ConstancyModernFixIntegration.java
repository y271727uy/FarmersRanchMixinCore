package com.y271727uy.FRMC.integration.constancy;

import me.pepperbell.continuity.client.mixinterface.ModelLoaderExtension;
import me.pepperbell.continuity.client.resource.ModelWrappingHandler;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.modernfix.ModernFixClient;
import org.embeddedt.modernfix.api.entrypoint.ModernFixClientIntegration;

import java.util.function.Function;

/**
 * Moves Constancy's model wrapping into ModernFix's per-model bake pipeline.
 *
 * <p>The matching Mixin disables Constancy's later full-map
 * ModelEvent.ModifyBakingResult scan, so each model is wrapped exactly once.</p>
 */
public final class ConstancyModernFixIntegration implements ModernFixClientIntegration {
    private static final ConstancyModernFixIntegration INSTANCE = new ConstancyModernFixIntegration();
    private static volatile boolean CALLBACK_OBSERVED;

    private ConstancyModernFixIntegration() {
    }

    public static void register() {
        if (!ModernFixClient.CLIENT_INTEGRATIONS.contains(INSTANCE)) {
            ModernFixClient.CLIENT_INTEGRATIONS.add(INSTANCE);
        }
    }

    public static boolean isCallbackObserved() {
        return CALLBACK_OBSERVED;
    }

    @Override
    public BakedModel onBakedModelLoad(
            ResourceLocation location,
            UnbakedModel baseModel,
            BakedModel originalModel,
            ModelState modelState,
            ModelBakery bakery,
            Function<Material, TextureAtlasSprite> textureGetter) {
        CALLBACK_OBSERVED = true;
        if (originalModel == null || !(bakery instanceof ModelLoaderExtension extension)) {
            return originalModel;
        }

        ModelWrappingHandler handler = extension.continuity$getModelWrappingHandler();
        return handler == null ? originalModel : handler.wrap(originalModel, location);
    }
}
