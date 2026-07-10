package com.y271727uy.FRMC.mixin.mystiasizakaya;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "org.hiedacamellia.mystiasizakaya.core.network.OpenMessage", remap = false)
public abstract class OpenMessageMixin {
    private static final String FRMC$PLAYER_EVENT_CLASS = "org.hiedacamellia.mystiasizakaya.core.event.MIPlayerEvent";
    private static final BlockPos FRMC$EMPTY_TABLE_POS = new BlockPos(-1, -1, -1);
    private static final String FRMC$FAILED_TABLE = "network.mystiasizakaya.ledger.failed.table";
    private static final String FRMC$FAILED_CUISINES = "network.mystiasizakaya.ledger.failed.cuisines";
    private static final String FRMC$FAILED_BEVERAGES = "network.mystiasizakaya.ledger.failed.beverages";

    @Redirect(
        method = "lambda$handleServer$4",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;sendSystemMessage(Lnet/minecraft/network/chat/Component;)V"
        ),
        require = 0,
        expect = 0
    )
    private static void frmc$onlySendUnsatisfiedLedgerFailure(ServerPlayer player, Component component) {
        String key = frmc$getTranslationKey(component);
        if (FRMC$FAILED_TABLE.equals(key) && frmc$hasBoundTable(player)) {
            return;
        }

        if (FRMC$FAILED_CUISINES.equals(key) && frmc$hasMenuEntry(player, true)) {
            return;
        }

        if (FRMC$FAILED_BEVERAGES.equals(key) && frmc$hasMenuEntry(player, false)) {
            return;
        }

        player.sendSystemMessage(component);
    }

    private static String frmc$getTranslationKey(Component component) {
        ComponentContents contents = component.getContents();
        if (contents instanceof TranslatableContents translatableContents) {
            return translatableContents.getKey();
        }

        return "";
    }

    private static boolean frmc$hasBoundTable(ServerPlayer player) {
        for (Object blockPos : frmc$getPlayerList(player, "getTables")) {
            if (blockPos instanceof BlockPos pos && !Objects.equals(pos, FRMC$EMPTY_TABLE_POS)) {
                return true;
            }
        }

        return false;
    }

    private static boolean frmc$hasMenuEntry(ServerPlayer player, boolean cuisine) {
        for (Object object : frmc$getPlayerList(player, cuisine ? "getMenus" : "getMenusBeverages")) {
            if (!(object instanceof String entry)) {
                continue;
            }

            if (entry != null && !entry.isBlank() && !"minecraft:air".equals(entry)) {
                return true;
            }
        }

        return false;
    }

    private static List<?> frmc$getPlayerList(ServerPlayer player, String methodName) {
        try {
            Class<?> playerEventClass = Class.forName(FRMC$PLAYER_EVENT_CLASS);
            Method accessor = playerEventClass.getMethod(methodName, net.minecraft.world.entity.player.Player.class);
            Object value = accessor.invoke(null, player);
            if (value instanceof List<?> list) {
                return list;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }

        return List.of();
    }
}
