package com.y271727uy.FRMC.mixin.mixinsquared.oculus;

import com.bawnorton.mixinsquared.adjuster.MixinAnnotationAdjusterRegistrar;
import com.bawnorton.mixinsquared.adjuster.tools.AdjustableInjectNode;
import org.spongepowered.asm.mixin.injection.Inject;

/**
 * Oculus 的 MixinProgram.iris$causeException注入到了
 * compileShaderInternal中，但没有设置 cancellable = true，然后在着色器编译失败时调用 cir.cancel()
 * Mixin 会在真正的编译错误之上抛出 CancellationException
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
