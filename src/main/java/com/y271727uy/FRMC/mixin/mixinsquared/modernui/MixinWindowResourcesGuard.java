package com.y271727uy.FRMC.mixin.mixinsquared.modernui;

import com.bawnorton.mixinsquared.adjuster.MixinAnnotationAdjusterRegistrar;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Inject;

public final class MixinWindowResourcesGuard {
    private static final String MIXIN_WINDOW = "icyllis.modernui.mc.mixin.MixinWindow";
    private static final String ON_SET_GUI_SCALE = "onSetGuiScale";
    private static final String MODERN_UI_OWNER = "icyllis/modernui/ModernUI";
    private static final String GET_RESOURCES_DESC = "()Licyllis/modernui/resources/Resources;";
    private static final String RESOURCES_OWNER = "icyllis/modernui/resources/Resources";
    private static final String UPDATE_METRICS_DESC = "(Licyllis/modernui/util/DisplayMetrics;)V";
    private static boolean registered;

    private MixinWindowResourcesGuard() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        MixinAnnotationAdjusterRegistrar.register((targetClassNames, mixinClassName, handlerNode, annotationNode) -> {
            if (!MIXIN_WINDOW.equals(mixinClassName)
                    || !ON_SET_GUI_SCALE.equals(handlerNode.name)
                    || !annotationNode.is(Inject.class)) {
                return annotationNode;
            }
            guardNullResources(handlerNode);
            return annotationNode;
        });
    }

    private static void guardNullResources(MethodNode handlerNode) {
        AbstractInsnNode[] instructions = handlerNode.instructions.toArray();
        for (int index = 0; index < instructions.length; index++) {
            if (!(instructions[index] instanceof MethodInsnNode getResources)
                    || getResources.getOpcode() != Opcodes.INVOKEVIRTUAL
                    || !MODERN_UI_OWNER.equals(getResources.owner)
                    || !"getResources".equals(getResources.name)
                    || !GET_RESOURCES_DESC.equals(getResources.desc)) {
                continue;
            }

            MethodInsnNode updateMetrics = findUpdateMetrics(instructions, index + 1);
            if (updateMetrics == null) {
                return;
            }

            LabelNode skipUpdate = new LabelNode();
            LabelNode afterUpdate = new LabelNode();

            InsnList skipBlock = new InsnList();
            skipBlock.add(new JumpInsnNode(Opcodes.GOTO, afterUpdate));
            skipBlock.add(skipUpdate);
            skipBlock.add(new InsnNode(Opcodes.POP));
            skipBlock.add(afterUpdate);
            handlerNode.instructions.insert(updateMetrics, skipBlock);

            InsnList nullCheck = new InsnList();
            nullCheck.add(new InsnNode(Opcodes.DUP));
            nullCheck.add(new JumpInsnNode(Opcodes.IFNULL, skipUpdate));
            handlerNode.instructions.insert(getResources, nullCheck);
            return;
        }
    }

    private static MethodInsnNode findUpdateMetrics(AbstractInsnNode[] instructions, int startIndex) {
        for (int index = startIndex; index < instructions.length; index++) {
            if (instructions[index] instanceof MethodInsnNode method
                    && method.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && RESOURCES_OWNER.equals(method.owner)
                    && "updateMetrics".equals(method.name)
                    && UPDATE_METRICS_DESC.equals(method.desc)) {
                return method;
            }
        }
        return null;
    }
}
