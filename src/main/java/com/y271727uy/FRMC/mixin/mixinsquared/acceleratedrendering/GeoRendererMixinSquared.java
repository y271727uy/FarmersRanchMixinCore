package com.y271727uy.FRMC.mixin.mixinsquared.acceleratedrendering;

import com.bawnorton.mixinsquared.adjuster.MixinAnnotationAdjusterRegistrar;
import com.bawnorton.mixinsquared.adjuster.tools.AdjustableInjectNode;
import com.y271727uy.frmc.mixinfabricextras.injector.FabricInject;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.Inject;

public final class GeoRendererMixinSquared {
    private static final String GEO_RENDERER_MIXIN =
            "com.github.argon4w.acceleratedrendering.features.geckolib.mixins.GeoRendererMixin";
    private static boolean registered;

    private GeoRendererMixinSquared() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        MixinAnnotationAdjusterRegistrar.register((targetClassNames, mixinClassName, handlerNode, annotationNode) -> {
            if (!GEO_RENDERER_MIXIN.equals(mixinClassName) || !annotationNode.is(Inject.class)) {
                return annotationNode;
            }
            AdjustableInjectNode adjusted = annotationNode.as(AdjustableInjectNode.class);
            adjusted.desc = Type.getDescriptor(FabricInject.class);
            return adjusted;
        });
    }
}
