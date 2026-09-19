package com.y271727uy.FRMC.mixin.mixinsquared.oculus;

import com.bawnorton.mixinsquared.adjuster.MixinAnnotationAdjusterRegistrar;
import com.bawnorton.mixinsquared.adjuster.tools.AdjustableInjectNode;
import org.spongepowered.asm.mixin.injection.Inject;

/**
 * Oculus {@code MixinProgram.iris$causeException} injects into
 * {@code compileShaderInternal} without {@code cancellable = true}, then
 * calls {@code cir.cancel()} when a shader fails. Mixin throws
 * {@code CancellationException} on top of the real compile error.
 */
public final class MixinProgramCancellableGuard {
    private static final String OCULUS_PROGRAM_MIXIN = "net.irisshaders.iris.mixin.MixinProgram";
    private static final String CAUSE_EXCEPTION = "iris$causeException";
    private static boolean registered;

    private MixinProgramCancellableGuard() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        MixinAnnotationAdjusterRegistrar.register((targetClassNames, mixinClassName, handlerNode, annotationNode) -> {
            if (!OCULUS_PROGRAM_MIXIN.equals(mixinClassName)
                    || !CAUSE_EXCEPTION.equals(handlerNode.name)
                    || !annotationNode.is(Inject.class)) {
                return annotationNode;
            }
            AdjustableInjectNode injectNode = annotationNode.as(AdjustableInjectNode.class);
            injectNode.setCancellable(true);
            return injectNode;
        });
    }
}
