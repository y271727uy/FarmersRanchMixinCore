package com.y271727uy.FRMC.mixin.mixinsquared.manors_bounty;

import com.bawnorton.mixinsquared.adjuster.MixinAnnotationAdjusterRegistrar;
import com.bawnorton.mixinsquared.adjuster.tools.AdjustableAtNode;
import com.bawnorton.mixinsquared.adjuster.tools.AdjustableInjectNode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Inject;

import java.util.List;

public final class PoisonEffectMixinSquared {
    private static final String MANORS_BOUNTY_POISON_MIXIN = "net.mcreator.manors_bounty.mixins.PoisonEffectMixin";
    private static final String POISON_HANDLER = "onApplyEffect";
    private static final String APPLY_EFFECT_TICK_TARGET = "Lnet/minecraft/world/effect/MobEffect;m_6742_(Lnet/minecraft/world/entity/LivingEntity;I)V";
    private static final String LOGGER_OWNER = "org/slf4j/Logger";
    private static final String LOGGER_INFO_DESC = "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V";
    private static final String SEND_PARTICLES_OWNER = "net/minecraft/server/level/ServerLevel";
    private static final String SEND_PARTICLES_DESC = "(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I";
    private static boolean registered;

    private PoisonEffectMixinSquared() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        MixinAnnotationAdjusterRegistrar.register((targetClassNames, mixinClassName, handlerNode, annotationNode) -> {
            if (!MANORS_BOUNTY_POISON_MIXIN.equals(mixinClassName)
                    || !POISON_HANDLER.equals(handlerNode.name)
                    || !annotationNode.is(Inject.class)) {
                return annotationNode;
            }

            AdjustableInjectNode injectNode = annotationNode.as(AdjustableInjectNode.class);
            AdjustableAtNode at = AdjustableAtNode.InjectionPoint.INVOKE.toNode();
            at.setTarget(APPLY_EFFECT_TICK_TARGET);
            injectNode.setAt(List.of(at));

            removePoisonHealLog(handlerNode);
            reducePoisonHealParticles(handlerNode);
            return injectNode;
        });
    }

    private static void removePoisonHealLog(MethodNode handlerNode) {
        for (AbstractInsnNode instruction : handlerNode.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode method)
                    || !LOGGER_OWNER.equals(method.owner)
                    || !"info".equals(method.name)
                    || !LOGGER_INFO_DESC.equals(method.desc)) {
                continue;
            }

            InsnList pops = new InsnList();
            pops.add(new InsnNode(Opcodes.POP));
            pops.add(new InsnNode(Opcodes.POP));
            pops.add(new InsnNode(Opcodes.POP));
            pops.add(new InsnNode(Opcodes.POP));
            handlerNode.instructions.insertBefore(method, pops);
            handlerNode.instructions.remove(method);
            return;
        }
    }

    private static void reducePoisonHealParticles(MethodNode handlerNode) {
        boolean foundSendParticles = false;
        for (AbstractInsnNode instruction : handlerNode.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode method
                    && SEND_PARTICLES_OWNER.equals(method.owner)
                    && ("sendParticles".equals(method.name) || "m_8767_".equals(method.name))
                    && SEND_PARTICLES_DESC.equals(method.desc)) {
                foundSendParticles = true;
                break;
            }
        }

        if (!foundSendParticles) {
            return;
        }

        for (AbstractInsnNode instruction : handlerNode.instructions.toArray()) {
            if (instruction instanceof IntInsnNode intInstruction
                    && intInstruction.getOpcode() == Opcodes.BIPUSH
                    && intInstruction.operand == 6) {
                intInstruction.operand = 3;
                return;
            }
        }
    }
}
