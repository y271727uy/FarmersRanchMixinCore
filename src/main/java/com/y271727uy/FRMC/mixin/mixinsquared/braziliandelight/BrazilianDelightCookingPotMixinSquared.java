package com.y271727uy.FRMC.mixin.mixinsquared.braziliandelight;

import com.bawnorton.mixinsquared.adjuster.MixinAnnotationAdjusterRegistrar;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Inject;

public final class BrazilianDelightCookingPotMixinSquared {
    private static final String BRAZILIAN_COOKING_POT_MIXIN =
        "com.dannbrown.braziliandelight.mixin.CookingPotBlockMixin";
    private static final String USE_HANDLER = "brazilianDelight$use";
    private static final String PLAYER_OWNER = "net/minecraft/world/entity/player/Player";
    private static final String SET_ITEM_IN_HAND = "setItemInHand";
    private static final String SET_ITEM_IN_HAND_OBF = "m_21008_";
    private static final String SET_ITEM_IN_HAND_DESC =
        "(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V";
    private static final String HELPER_OWNER =
        "com/y271727uy/FRMC/integration/braziliandelight/ContainerStackHelper";
    private static final String HELPER_NAME = "replaceStackedContainer";
    private static final String HELPER_DESC =
        "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V";
    private static boolean registered;

    private BrazilianDelightCookingPotMixinSquared() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        MixinAnnotationAdjusterRegistrar.register((targetClassNames, mixinClassName, handlerNode, annotationNode) -> {
            if (!BRAZILIAN_COOKING_POT_MIXIN.equals(mixinClassName)
                    || !USE_HANDLER.equals(handlerNode.name)
                    || !annotationNode.is(Inject.class)) {
                return annotationNode;
            }

            replaceContainerCalls(handlerNode);
            return annotationNode;
        });
    }

    private static void replaceContainerCalls(MethodNode handlerNode) {
        for (var instruction : handlerNode.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode method)
                    || method.getOpcode() != Opcodes.INVOKEVIRTUAL
                    || !PLAYER_OWNER.equals(method.owner)
                    || !(SET_ITEM_IN_HAND.equals(method.name) || SET_ITEM_IN_HAND_OBF.equals(method.name))
                    || !SET_ITEM_IN_HAND_DESC.equals(method.desc)) {
                continue;
            }

            method.setOpcode(Opcodes.INVOKESTATIC);
            method.owner = HELPER_OWNER;
            method.name = HELPER_NAME;
            method.desc = HELPER_DESC;
            method.itf = false;
        }
    }

}
