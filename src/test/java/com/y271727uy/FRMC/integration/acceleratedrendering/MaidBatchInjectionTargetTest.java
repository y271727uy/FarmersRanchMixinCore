package com.y271727uy.FRMC.integration.acceleratedrendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector.Configure;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;

class MaidBatchInjectionTargetTest {
    @Test
    void selectedRenderOverloadContainsExactlyOneBatchInvocation() throws IOException {
        ClassNode mixin = readClass("com/y271727uy/FRMC/mixin/acceleratedrendering/touhoulittlemaid/IGeoRendererBatchMixin");
        MethodNode handler = mixin.methods.stream()
                .filter(method -> method.name.equals("frmc$batchMaidModel"))
                .findFirst().orElseThrow();
        List<AnnotationNode> annotations = new ArrayList<>();
        if (handler.visibleAnnotations != null) {
            annotations.addAll(handler.visibleAnnotations);
        }
        if (handler.invisibleAnnotations != null) {
            annotations.addAll(handler.invisibleAnnotations);
        }
        AnnotationNode wrap = annotations.stream()
                .filter(annotation -> annotation.desc.equals(
                        "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;"))
                .findFirst().orElseThrow();
        List<String> selectors = value(wrap, "method");
        assertEquals(1, selectors.size());
        assertEquals(1, (Integer) value(wrap, "require"));
        MemberInfo selector = (MemberInfo) MemberInfo.parse(selectors.get(0), null)
                .validate().configure(Configure.SELECT_MEMBER);
        Object atValue = value(wrap, "at");
        AnnotationNode at = (AnnotationNode) (atValue instanceof List<?> list ? list.get(0) : atValue);
        MemberInfo invocation = MemberInfo.parse(value(at, "target"), null).validate();

        // Read bytecode without loading Minecraft's renderer or starting the game.
        ClassNode renderer = readClass("com/github/tartaricacid/touhoulittlemaid/geckolib3/geo/GeoReplacedEntityRenderer");
        List<MethodNode> targets = renderer.methods.stream()
                .filter(method -> selector.matches(renderer.name, method.name, method.desc).isExactMatch())
                .limit(selector.getMaxMatchCount())
                .toList();
        assertEquals(1, targets.size(), "The batch hook must select one concrete render overload");
        MethodNode target = targets.get(0);
        int calls = 0;
        for (var instruction : target.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && invocation.matches(call.owner, call.name, call.desc).isExactMatch()) {
                calls++;
            }
        }
        assertEquals(1, calls, "No unique batch invocation in selected method: " + target.name + target.desc);
    }

    private static ClassNode readClass(String internalName) throws IOException {
        try (InputStream input = MaidBatchInjectionTargetTest.class.getClassLoader()
                .getResourceAsStream(internalName + ".class")) {
            assertNotNull(input, "Missing class on test classpath: " + internalName);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T value(AnnotationNode annotation, String key) {
        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (key.equals(annotation.values.get(i))) {
                return (T) annotation.values.get(i + 1);
            }
        }
        throw new AssertionError("Missing annotation value: " + key);
    }
}
