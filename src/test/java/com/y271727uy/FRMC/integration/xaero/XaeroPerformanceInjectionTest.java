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
    void minimapLimiterWrapsOnlyOffscreenDrawingAndHasValidResetHooks() throws IOException {
        ClassNode mixin = readClass("com/y271727uy/FRMC/mixin/xaero/minimap/MinimapFboRefreshMixin");
        ClassNode target = readClass("xaero/common/minimap/render/MinimapFBORenderer");
        MethodNode wrapper = mixin.methods.stream()
                .filter(method -> method.name.equals("frmc$refreshCachedMap")).findFirst().orElseThrow();
        AnnotationNode wrap = annotations(wrapper).stream()
                .filter(annotation -> annotation.desc.endsWith("/WrapMethod;")).findFirst().orElseThrow();
        List<String> selectors = value(wrap, "method");
        assertEquals(List.of("renderChunksToFBO"), selectors);
        MethodNode original = target.methods.stream()
                .filter(method -> method.name.equals(selectors.get(0))).findFirst().orElseThrow();
        Type[] wrapperArguments = Type.getArgumentTypes(wrapper.desc);
        assertEquals("Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;",
                wrapperArguments[wrapperArguments.length - 1].getDescriptor());
        Type[] forwardedArguments = java.util.Arrays.copyOf(wrapperArguments, wrapperArguments.length - 1);
        assertEquals(original.desc, Type.getMethodDescriptor(Type.VOID_TYPE, forwardedArguments));

        MethodNode invalidator = mixin.methods.stream()
                .filter(method -> method.name.equals("frmc$invalidateCachedMap")).findFirst().orElseThrow();
        AnnotationNode inject = annotations(invalidator).stream()
                .filter(annotation -> annotation.desc.endsWith("/Inject;")).findFirst().orElseThrow();
        List<String> resetMethods = value(inject, "method");
        assertEquals(3, resetMethods.size());
        for (String reset : resetMethods) {
            assertEquals(1L, target.methods.stream().filter(method -> method.name.equals(reset)).count(), reset);
        }
        for (var shadow : mixin.fields) {
            boolean isShadow = shadow.visibleAnnotations != null && shadow.visibleAnnotations.stream()
                    .anyMatch(annotation -> annotation.desc.endsWith("/Shadow;"));
            isShadow |= shadow.invisibleAnnotations != null && shadow.invisibleAnnotations.stream()
                    .anyMatch(annotation -> annotation.desc.endsWith("/Shadow;"));
            if (isShadow) {
                assertTrue(target.fields.stream().anyMatch(field -> field.name.equals(shadow.name)
                        && field.desc.equals(shadow.desc)), "Unresolved shadow field: " + shadow.name);
            }
        }
        // The outer draw must keep executing after the wrapped method. It
        // restores the main render target and binds the cached rotation texture.
        MethodNode outer = target.methods.stream().filter(method -> method.name.equals("renderChunks"))
                .findFirst().orElseThrow();
        int wrappedCalls = 0;
        boolean restorationAfterDraw = false;
        for (var instruction : outer.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.name.equals("renderChunksToFBO") && call.desc.equals(original.desc)) wrappedCalls++;
                if (wrappedCalls > 0 && call.name.equals("restoreDefaultTarget")) restorationAfterDraw = true;
            }
        }
        assertEquals(1, wrappedCalls);
        assertTrue(restorationAfterDraw);
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
