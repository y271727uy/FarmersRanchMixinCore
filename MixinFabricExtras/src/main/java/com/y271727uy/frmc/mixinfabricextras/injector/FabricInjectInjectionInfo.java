package com.y271727uy.frmc.mixinfabricextras.injector;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.callback.CallbackInjector;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.CallbackInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;

@InjectionInfo.AnnotationType(FabricInject.class)
public final class FabricInjectInjectionInfo extends CallbackInjectionInfo {
    public FabricInjectInjectionInfo(MixinTargetContext context, MethodNode handler, AnnotationNode annotation) {
        super(context, handler, annotation);
    }

    @Override
    protected Injector parseInjector(AnnotationNode annotation) {
        boolean cancellable = Annotations.getValue(annotation, "cancellable", Boolean.FALSE);
        LocalCapture locals = Annotations.getValue(annotation, "locals", LocalCapture.class, LocalCapture.NO_CAPTURE);
        String id = Annotations.getValue(annotation, "id", "");
        return new CallbackInjector(this, cancellable, locals, id);
    }
}
