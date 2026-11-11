package com.y271727uy.frmc.mixinfabricextras.transformer;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.Frame;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricInjectExtensionTest {
    private static final String OWNER = "test/GeoRenderer";
    private static final String HANDLER = "renderFast";

    @Test
    void emitsInterfaceCallbackAndCancellationReturn() throws Exception {
        ClassNode target = targetClass(true);

        new FabricInjectExtension().postApply(context(target));

        MethodNode render = findMethod(target, "render");
        List<Integer> opcodes = new ArrayList<>();
        for (var instruction : render.instructions) {
            opcodes.add(instruction.getOpcode());
        }

        assertEquals(1, opcodes.stream().filter(opcode -> opcode == Opcodes.INVOKEINTERFACE).count());
        assertTrue(opcodes.contains(Opcodes.INVOKEINTERFACE));
        assertTrue(opcodes.contains(Opcodes.IFEQ));
        assertTrue(opcodes.contains(Opcodes.RETURN));
        assertTrue(findMethod(target, HANDLER).visibleAnnotations == null || findMethod(target, HANDLER).visibleAnnotations.isEmpty());

        Analyzer<?> analyzer = new Analyzer<>(new BasicInterpreter());
        Frame<?>[] frames = analyzer.analyze(OWNER, render);
        assertNotNull(frames);
    }

    @Test
    void insertsCancellableHeadCallbackBeforeTheFirstOriginalInstruction() throws Exception {
        ClassNode target = targetClass(true);
        MethodNode render = findMethod(target, "render");
        var originalFirst = render.instructions.getFirst();
        AnnotationNode head = new AnnotationNode(Type.getDescriptor(At.class));
        head.values = List.of("value", "HEAD");
        AnnotationNode handler = findMethod(target, HANDLER).visibleAnnotations.get(0);
        handler.values.set(handler.values.indexOf("at") + 1, List.of(head));

        new FabricInjectExtension().postApply(context(target));

        assertEquals(Opcodes.ACONST_NULL, originalFirst.getOpcode());
        assertEquals(Opcodes.IFEQ, originalFirst.getPrevious().getPrevious().getPrevious().getOpcode());
        new Analyzer<>(new BasicInterpreter()).analyze(OWNER, render);
    }

    @Test
    void emitsNonCancellableCallbackWithoutEarlyReturn() throws Exception {
        ClassNode target = targetClass(false);

        new FabricInjectExtension().postApply(context(target));

        MethodNode render = findMethod(target, "render");
        List<Integer> opcodes = new ArrayList<>();
        for (var instruction : render.instructions) {
            opcodes.add(instruction.getOpcode());
        }

        assertTrue(opcodes.contains(Opcodes.INVOKEINTERFACE));
        assertTrue(opcodes.stream().noneMatch(opcode -> opcode == Opcodes.IFEQ));
        Analyzer<?> analyzer = new Analyzer<>(new BasicInterpreter());
        analyzer.analyze(OWNER, render);
    }

    @Test
    void supportsConcreteTargetClass() {
        ClassNode target = targetClass(false, false);

        new FabricInjectExtension().postApply(context(target));

        MethodNode render = findMethod(target, "render");
        boolean hasVirtualHandlerCall = false;
        for (var instruction : render.instructions) {
            if (instruction instanceof MethodInsnNode method
                    && method.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && method.name.equals(HANDLER)) {
                hasVirtualHandlerCall = true;
            }
        }
        assertTrue(hasVirtualHandlerCall);
    }

    private static ClassNode targetClass(boolean cancellable) {
        return targetClass(cancellable, true);
    }

    private static ClassNode targetClass(boolean cancellable, boolean targetIsInterface) {
        ClassNode target = new ClassNode();
        target.version = Opcodes.V17;
        target.access = Opcodes.ACC_PUBLIC | (targetIsInterface ? Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT : 0);
        target.name = OWNER;
        target.superName = "java/lang/Object";

        MethodNode render = new MethodNode(Opcodes.ACC_PUBLIC, "render", "()V", null, null);
        render.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        render.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "test/GeoBone", "getCubes", "()Ljava/util/List;", false));
        render.instructions.add(new InsnNode(Opcodes.POP));
        render.instructions.add(new InsnNode(Opcodes.RETURN));
        render.maxStack = 1;
        render.maxLocals = 1;
        target.methods.add(render);

        String handlerDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(CallbackInfo.class));
        MethodNode handler = new MethodNode(Opcodes.ACC_PUBLIC, HANDLER, handlerDescriptor, null, null);
        handler.instructions.add(new InsnNode(Opcodes.RETURN));
        handler.maxStack = 0;
        handler.maxLocals = 2;
        handler.visibleAnnotations = new ArrayList<>();
        handler.visibleAnnotations.add(fabricInject(cancellable));
        target.methods.add(handler);
        return target;
    }

    private static AnnotationNode fabricInject(boolean cancellable) {
        AnnotationNode annotation = new AnnotationNode("Lcom/y271727uy/frmc/mixinfabricextras/injector/FabricInject;");
        annotation.values = new ArrayList<>();
        annotation.values.add("method");
        annotation.values.add(List.of("render"));
        annotation.values.add("cancellable");
        annotation.values.add(cancellable);

        AnnotationNode at = new AnnotationNode(Type.getDescriptor(At.class));
        at.values = new ArrayList<>();
        at.values.add("value");
        at.values.add("INVOKE");
        at.values.add("target");
        at.values.add("Ltest/GeoBone;getCubes()Ljava/util/List;");
        at.values.add("shift");
        at.values.add(new String[]{Type.getDescriptor(At.Shift.class), "BEFORE"});

        annotation.values.add("at");
        annotation.values.add(List.of(at));
        return annotation;
    }

    private static ITargetClassContext context(ClassNode target) {
        return (ITargetClassContext) Proxy.newProxyInstance(
                ITargetClassContext.class.getClassLoader(),
                new Class<?>[]{ITargetClassContext.class},
                (proxy, method, arguments) -> method.getName().equals("getClassNode") ? target : null
        );
    }

    private static MethodNode findMethod(ClassNode classNode, String name) {
        return classNode.methods.stream().filter(method -> method.name.equals(name)).findFirst().orElseThrow();
    }
}
