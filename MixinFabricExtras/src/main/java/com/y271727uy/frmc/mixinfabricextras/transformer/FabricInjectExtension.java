package com.y271727uy.frmc.mixinfabricextras.transformer;

import com.y271727uy.frmc.mixinfabricextras.injector.FabricInject;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.util.ArrayList;
import java.util.List;

public final class FabricInjectExtension implements IExtension {
    @Override
    public boolean checkActive(MixinEnvironment environment) {
        return true;
    }

    @Override
    public void preApply(ITargetClassContext context) {
    }

    @Override
    public void postApply(ITargetClassContext context) {
        ClassNode targetClass = context.getClassNode();
        List<MethodNode> handlers = new ArrayList<>();
        for (MethodNode method : targetClass.methods) {
            if (findAnnotation(method, Type.getDescriptor(FabricInject.class)) != null) {
                handlers.add(method);
            }
        }

        for (MethodNode handler : handlers) {
            AnnotationNode annotation = findAnnotation(handler, Type.getDescriptor(FabricInject.class));
            applyHandler(targetClass, handler, annotation);
            handler.visibleAnnotations.remove(annotation);
        }
    }

    private static void applyHandler(ClassNode targetClass, MethodNode handler, AnnotationNode annotation) {
        List<String> targetMethods = annotationValue(annotation, "method");
        List<AnnotationNode> atNodes = annotationValue(annotation, "at");
        Boolean cancellable = annotationValue(annotation, "cancellable", Boolean.FALSE);

        if (targetMethods == null || targetMethods.isEmpty() || atNodes == null || atNodes.isEmpty()) {
            throw new IllegalArgumentException("@FabricInject requires at least one method selector and one @At: " + handler.name);
        }

        String callbackId = annotationValue(annotation, "id", "");
        int matches = 0;
        for (String targetName : targetMethods) {
            MethodNode targetMethod = findMethod(targetClass, targetName);
            if (targetMethod == null) {
                throw new IllegalStateException("@FabricInject target method not found: " + targetName + " in " + targetClass.name);
            }
            if ((handler.access & Opcodes.ACC_STATIC) != 0 || (targetMethod.access & Opcodes.ACC_STATIC) != 0) {
                throw new IllegalStateException("@FabricInject currently requires instance handler and target methods: " + handler.name);
            }
            if (!"V".equals(Type.getReturnType(targetMethod.desc).getDescriptor())) {
                throw new IllegalStateException("@FabricInject currently supports only void target methods: " + targetName);
            }
            String expectedHandlerDescriptor = Type.getMethodDescriptor(
                    Type.VOID_TYPE,
                    append(Type.getArgumentTypes(targetMethod.desc), Type.getObjectType(Type.getInternalName(CallbackInfo.class)))
            );
            if (!expectedHandlerDescriptor.equals(handler.desc)) {
                throw new IllegalStateException("@FabricInject handler descriptor mismatch: expected " + expectedHandlerDescriptor + " but found " + handler.desc);
            }

            for (AnnotationNode at : atNodes) {
                matches += applyAt(targetClass, targetMethod, handler, at, callbackId.isEmpty() ? targetName : callbackId, cancellable);
            }
        }

        if (matches == 0) {
            throw new IllegalStateException("@FabricInject found no matching injection point in " + targetClass.name + " for " + handler.name);
        }
        Integer require = annotationValue(annotation, "require", -1);
        Integer allow = annotationValue(annotation, "allow", -1);
        if (require >= 0 && matches < require) {
            throw new IllegalStateException("@FabricInject matched " + matches + " injection points but requires " + require + ": " + handler.name);
        }
        if (allow >= 0 && matches > allow) {
            throw new IllegalStateException("@FabricInject matched " + matches + " injection points but allows at most " + allow + ": " + handler.name);
        }
    }

    private static int applyAt(ClassNode targetClass, MethodNode targetMethod, MethodNode handler,
                                AnnotationNode at, String callbackId, boolean cancellable) {
        String value = annotationValue(at, "value");
        if ("HEAD".equals(value)) {
            insertCallback(targetClass, targetMethod, handler, callbackId, cancellable,
                    targetMethod.instructions.getFirst(), true);
            return 1;
        }
        if (!"INVOKE".equals(value)) {
            throw new IllegalArgumentException("@FabricInject supports only HEAD and INVOKE: " + handler.name);
        }
        String target = annotationValue(at, "target");
        int ordinal = annotationValue(at, "ordinal", -1);
        At.Shift shift = annotationEnum(at, "shift", At.Shift.class, At.Shift.NONE);
        int by = annotationValue(at, "by", 0);
        if (shift == At.Shift.BY && by <= 0) {
            throw new IllegalArgumentException("@FabricInject @At(shift = BY) requires a positive by value: " + handler.name);
        }

        int matches = 0;
        int ordinalIndex = 0;
        for (AbstractInsnNode instruction : targetMethod.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode call) || !matches(call, target)) {
                continue;
            }
            if (ordinal >= 0 && ordinal != ordinalIndex++) {
                continue;
            }
            AbstractInsnNode insertionPoint = insertionPoint(targetMethod, instruction, shift, by);
            insertCallback(targetClass, targetMethod, handler, callbackId, cancellable, insertionPoint,
                    shift == At.Shift.NONE || shift == At.Shift.BEFORE);
            matches++;
        }
        return matches;
    }

    private static void insertCallback(ClassNode targetClass, MethodNode targetMethod, MethodNode handler,
                                       String callbackId, boolean cancellable, AbstractInsnNode point, boolean before) {
        int local = targetMethod.maxLocals++;
        Type[] arguments = Type.getArgumentTypes(targetMethod.desc);
        InsnList injected = callback(targetClass, handler, callbackId, cancellable, local, arguments);
        if (before) {
            targetMethod.instructions.insertBefore(point, injected);
        } else {
            targetMethod.instructions.insert(point, injected);
        }
        targetMethod.maxStack += local + 4;
    }

    private static AbstractInsnNode insertionPoint(MethodNode method, AbstractInsnNode instruction, At.Shift shift, int by) {
        if (shift == At.Shift.NONE || shift == At.Shift.BEFORE) {
            return instruction;
        }
        AbstractInsnNode point = instruction;
        int count = shift == At.Shift.BY ? by : 1;
        while (count-- > 0 && point != null) {
            point = point.getNext();
        }
        if (point == null) {
            throw new IllegalStateException("@FabricInject shift goes past the end of " + method.name);
        }
        return point;
    }

    private static InsnList callback(ClassNode targetClass, MethodNode handler,
                                     String callbackId, boolean cancellable, int local, Type[] arguments) {
        InsnList injected = new InsnList();
        injected.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(CallbackInfo.class)));
        injected.add(new InsnNode(Opcodes.DUP));
        injected.add(new LdcInsnNode(callbackId));
        injected.add(new InsnNode(cancellable ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        injected.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(CallbackInfo.class), "<init>", "(Ljava/lang/String;Z)V", false));
        injected.add(new VarInsnNode(Opcodes.ASTORE, local));
        injected.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int argumentLocal = 1;
        for (Type argument : arguments) {
            injected.add(new VarInsnNode(argument.getOpcode(Opcodes.ILOAD), argumentLocal));
            argumentLocal += argument.getSize();
        }
        injected.add(new VarInsnNode(Opcodes.ALOAD, local));
        boolean interfaceOwner = (targetClass.access & Opcodes.ACC_INTERFACE) != 0;
        injected.add(new MethodInsnNode(interfaceOwner ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL,
                targetClass.name, handler.name, handler.desc, interfaceOwner));
        if (cancellable) {
            injected.add(new VarInsnNode(Opcodes.ALOAD, local));
            injected.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(CallbackInfo.class), "isCancelled", "()Z", false));
            LabelNode skipReturn = new LabelNode();
            injected.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, skipReturn));
            injected.add(new InsnNode(Opcodes.RETURN));
            injected.add(skipReturn);
        }
        return injected;
    }

    private static MethodNode findMethod(ClassNode classNode, String selector) {
        int descriptorStart = selector.indexOf('(');
        String methodName = descriptorStart < 0 ? selector : selector.substring(0, descriptorStart);
        String descriptor = descriptorStart < 0 ? null : selector.substring(descriptorStart);
        MethodNode match = null;
        for (MethodNode method : classNode.methods) {
            if (method.name.equals(methodName) && (descriptor == null || method.desc.equals(descriptor))) {
                if (match != null) {
                    throw new IllegalStateException("@FabricInject selector is ambiguous: " + selector + " in " + classNode.name);
                }
                match = method;
            }
        }
        return match;
    }

    private static Type[] append(Type[] arguments, Type trailingArgument) {
        Type[] result = java.util.Arrays.copyOf(arguments, arguments.length + 1);
        result[arguments.length] = trailingArgument;
        return result;
    }

    private static AnnotationNode findAnnotation(MethodNode method, String descriptor) {
        if (method.visibleAnnotations == null) {
            return null;
        }
        for (AnnotationNode annotation : method.visibleAnnotations) {
            if (descriptor.equals(annotation.desc)) {
                return annotation;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T annotationValue(AnnotationNode annotation, String name) {
        return (T) annotationValue(annotation, name, null);
    }

    @SuppressWarnings("unchecked")
    private static <T> T annotationValue(AnnotationNode annotation, String name, T fallback) {
        if (annotation.values == null) {
            return fallback;
        }
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (name.equals(annotation.values.get(index))) {
                return (T) annotation.values.get(index + 1);
            }
        }
        return fallback;
    }

    private static <T extends Enum<T>> T annotationEnum(AnnotationNode annotation, String name, Class<T> enumType, T fallback) {
        String[] value = annotationValue(annotation, name);
        return value == null ? fallback : Enum.valueOf(enumType, value[1]);
    }

    private static boolean matches(MethodInsnNode call, String target) {
        if (target == null || target.isEmpty()) {
            return false;
        }
        int descriptorStart = target.indexOf('(');
        int ownerSeparator = target.indexOf(';');
        if (!target.startsWith("L") || ownerSeparator < 0 || descriptorStart < ownerSeparator) {
            return false;
        }
        String owner = target.substring(1, ownerSeparator);
        String name = target.substring(ownerSeparator + 1, descriptorStart);
        String descriptor = target.substring(descriptorStart);
        return call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(descriptor);
    }

    @Override
    public void export(MixinEnvironment environment, String name, boolean force, ClassNode classNode) {
    }
}
