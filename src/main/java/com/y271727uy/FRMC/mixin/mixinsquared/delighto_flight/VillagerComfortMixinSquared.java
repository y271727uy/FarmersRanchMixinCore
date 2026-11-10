package com.y271727uy.FRMC.mixin.mixinsquared.delighto_flight;

import com.bawnorton.mixinsquared.adjuster.MixinAnnotationAdjusterRegistrar;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.Inject;

public final class VillagerComfortMixinSquared {
    private static boolean registered;

    private VillagerComfortMixinSquared() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        MixinAnnotationAdjusterRegistrar.register((targets, mixin, handler, annotation) -> {
            if ("com.cloudmeow.delightoflight.mixin.VillagerMixin".equals(mixin)
                    && "startSleeping".equals(handler.name) && annotation.is(Inject.class)) {
                moveComfortWriteBeforeOfferLoop(handler);
            }
            return annotation;
        });
    }

    private static void moveComfortWriteBeforeOfferLoop(MethodNode handler) {
        MethodInsnNode getOffers = null;
        for (AbstractInsnNode instruction : handler.instructions) {
            if (instruction instanceof MethodInsnNode method
                    && "()Lnet/minecraft/world/item/trading/MerchantOffers;".equals(method.desc)
                    && ("getOffers".equals(method.name) || "m_6616_".equals(method.name))) {
                getOffers = method;
            }
            if (getOffers != null && instruction instanceof FieldInsnNode field
                    && field.getOpcode() == Opcodes.PUTFIELD && "Z".equals(field.desc)
                    && "delightoFlight$feelComfortable".equals(field.name)) {
                AbstractInsnNode value = field.getPrevious();
                AbstractInsnNode receiver = value.getPrevious();
                if (value.getOpcode() != Opcodes.ICONST_1 || !(receiver instanceof VarInsnNode load)
                        || load.getOpcode() != Opcodes.ALOAD || load.var != 0) {
                    return;
                }
                // Let getOffers initialize trades before changing the flag, then mark even an empty list.
                InsnList write = new InsnList();
                handler.instructions.remove(receiver);
                handler.instructions.remove(value);
                handler.instructions.remove(field);
                write.add(receiver);
                write.add(value);
                write.add(field);
                handler.instructions.insert(getOffers, write);
                handler.maxStack = Math.max(handler.maxStack, 3);
                return;
            }
        }
    }
}
