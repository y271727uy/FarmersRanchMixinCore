package com.y271727uy.FRMC.integration.xaero;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector.Configure;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;

class XaeroPerformanceInjectionTest {
    @Test
    void injectionSelectorsMatchTheInstalledXaeroBytecode() throws Exception {
        String mixinPackage = "com/y271727uy/FRMC/mixin/xaero/worldmap/";
        for (String[] entry : List.of(
                new String[]{"RegionTextureHeightArrayMixin", "xaero/map/region/texture/RegionTexture"},
                new String[]{"RegionTextureUploadMixin", "xaero/map/region/texture/RegionTexture"},
                new String[]{"MapLimiterPollingMixin", "xaero/map/MapLimiter"},
                new String[]{"MapWriterLayerMarksMixin", "xaero/map/MapWriter"},
                new String[]{"TextureUploadBenchmarkMixin", "xaero/map/graphics/TextureUploadBenchmark"})) {
            ClassNode mixin = readClass(mixinPackage + entry[0]);
            ClassNode target = readClass(entry[1]);
            int checked = 0;
            for (MethodNode handler : mixin.methods) {
                for (AnnotationNode annotation : annotations(handler)) {
                    boolean wrap = annotation.desc.endsWith("/WrapOperation;");
                    if (!wrap && !annotation.desc.endsWith("/Inject;")) {
                        continue;
                    }
                    List<String> selectors = value(annotation, "method");
                    assertEquals(1, selectors.size());
                    MemberInfo selector = (MemberInfo) MemberInfo.parse(selectors.get(0), null)
                            .validate().configure(Configure.SELECT_MEMBER);
                    List<MethodNode> methods = target.methods.stream()
                            .filter(method -> selector.matches(target.name, method.name, method.desc).isExactMatch())
                            .toList();
                    assertEquals(1, methods.size(), entry[0] + ": " + selectors);
                    assertTrue((Integer) value(annotation, "require") > 0);
                    if (wrap) {
                        Object atValue = value(annotation, "at");
                        AnnotationNode at = (AnnotationNode) (atValue instanceof List<?> list ? list.get(0) : atValue);
                        String atTarget = value(at, "target");
                        String kind = value(at, "value");
                        int count = 0;
                        MemberInfo invocation = "NEW".equals(kind) ? null : MemberInfo.parse(atTarget, null).validate();
                        for (var instruction : methods.get(0).instructions) {
                            if (!(instruction instanceof MethodInsnNode call)) {
                                continue;
                            }
                            if (invocation != null) {
                                if (invocation.matches(call.owner, call.name, call.desc).isExactMatch()) {
                                    count++;
                                }
                            } else if (call.name.equals("<init>")
                                    && call.owner.equals(Type.getReturnType(atTarget).getInternalName())
                                    && call.desc.equals(Type.getMethodDescriptor(Type.VOID_TYPE, Type.getArgumentTypes(atTarget)))) {
                                count++;
                            }
                        }
                        assertEquals((Integer) value(annotation, "require"), count, entry[0] + ": " + atTarget);
                    }
                    checked++;
                }
            }
            assertTrue(checked > 0, "No injection checked for " + entry[0]);
        }
    }

    @Test
    void arBridgeUsesTheInstalledGuiBatchApi() throws IOException {
        ClassNode controller = readClass("com/github/argon4w/acceleratedrendering/features/items/gui/GuiBatchingController");
        assertTrue(controller.methods.stream().anyMatch(method -> method.name.equals("startBatching")
                && method.desc.equals("(Lnet/minecraft/client/gui/GuiGraphics;)Z")));
        assertTrue(controller.methods.stream().anyMatch(method -> method.name.equals("flushBatching")
                && method.desc.equals("(Lnet/minecraft/client/gui/GuiGraphics;)F")));
    }

    private static List<AnnotationNode> annotations(MethodNode method) {
        List<AnnotationNode> result = new ArrayList<>();
        if (method.visibleAnnotations != null) result.addAll(method.visibleAnnotations);
        if (method.invisibleAnnotations != null) result.addAll(method.invisibleAnnotations);
        return result;
    }

    private static ClassNode readClass(String name) throws IOException {
        try (InputStream input = XaeroPerformanceInjectionTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, "Missing class on test classpath: " + name);
            ClassNode result = new ClassNode();
            new ClassReader(input).accept(result, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T value(AnnotationNode annotation, String name) {
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (name.equals(annotation.values.get(index))) return (T) annotation.values.get(index + 1);
        }
        throw new AssertionError("Missing annotation value: " + name);
    }
}
