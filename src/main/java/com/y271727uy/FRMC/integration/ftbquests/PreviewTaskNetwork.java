package com.y271727uy.FRMC.integration.ftbquests;

import com.y271727uy.FRMC.FRMCMod;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** Confirms a task only when the opened JEI identity matches the server-side task. */
public final class PreviewTaskNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FRMCMod.MODID, "preview_task"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private PreviewTaskNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, ConfirmPreviewPacket.class,
                ConfirmPreviewPacket::encode, ConfirmPreviewPacket::decode,
                ConfirmPreviewPacket::handle);
    }

    public static void confirm(long taskId, String recipeType, String recipe) {
        CHANNEL.sendToServer(new ConfirmPreviewPacket(taskId, recipeType, recipe));
    }

    private record ConfirmPreviewPacket(long taskId, String recipeType, String recipe) {
        private static void encode(ConfirmPreviewPacket packet, FriendlyByteBuf buffer) {
            buffer.writeLong(packet.taskId);
            buffer.writeUtf(packet.recipeType, 256);
            buffer.writeUtf(packet.recipe, 256);
        }

        private static ConfirmPreviewPacket decode(FriendlyByteBuf buffer) {
            return new ConfirmPreviewPacket(buffer.readLong(), buffer.readUtf(256), buffer.readUtf(256));
        }

        private static void handle(ConfirmPreviewPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
                context.setPacketHandled(true);
                return;
            }
            ServerPlayer player = context.getSender();
            context.enqueueWork(() -> complete(player, packet.taskId, packet.recipeType, packet.recipe));
            context.setPacketHandled(true);
        }
    }

    private static void complete(ServerPlayer player, long taskId, String recipeType, String recipe) {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (player == null || file == null) {
            return;
        }
        TeamData teamData = TeamData.get(player);
        Task task = file.getTask(taskId);
        if (teamData == null || teamData.isLocked() || !(task instanceof PreviewTask preview)
                || !preview.matches(recipeType, recipe)
                || !teamData.canStartTasks(preview.getQuest())) {
            return;
        }
        file.withPlayerContext(player, () -> preview.submitTask(teamData, player));
    }
}
