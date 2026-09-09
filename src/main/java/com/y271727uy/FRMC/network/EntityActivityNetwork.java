package com.y271727uy.FRMC.network;

import com.y271727uy.FRMC.entity.manager.entityactivity.EntityActivityManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class EntityActivityNetwork {
    public static final int CLIENT_PROTOCOL_VERSION = 2;
    private static final String NETWORK_PROTOCOL_VERSION = Integer.toString(CLIENT_PROTOCOL_VERSION);
    private static final int MAX_TRACKED_ENTITIES_PER_PLAYER = 4096;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("frmc", "entity_activity"),
            () -> NETWORK_PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(NETWORK_PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(NETWORK_PROTOCOL_VERSION));

    private static final Map<UUID, ClientCapabilities> CAPABILITIES = new HashMap<>();

    private EntityActivityNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ClientCapabilitiesPacket.class,
                ClientCapabilitiesPacket::encode, ClientCapabilitiesPacket::decode,
                ClientCapabilitiesPacket::handle);
        CHANNEL.registerMessage(id, EntityVisibilityBatchPacket.class,
                EntityVisibilityBatchPacket::encode, EntityVisibilityBatchPacket::decode,
                EntityVisibilityBatchPacket::handle);
    }

    public static void recordCapabilities(ServerPlayer player, ClientCapabilities capabilities) {
        CAPABILITIES.put(player.getUUID(), capabilities);
    }

    public static void recordVisibility(ServerPlayer player, int entityId, boolean visible) {
        ClientCapabilities capabilities = CAPABILITIES.get(player.getUUID());
        if (capabilities == null || !capabilities.chlorideVisibility()) {
            return;
        }
        if (capabilities.visibility().size() >= MAX_TRACKED_ENTITIES_PER_PLAYER
                && !capabilities.visibility().containsKey(entityId)) {
            return;
        }
        capabilities.visibility().put(entityId, visible);
        if (visible && player.serverLevel().getEntity(entityId) instanceof Mob mob) {
            EntityActivityManager.wake(mob);
        }
    }

    public static ClientCapabilities capabilities(ServerPlayer player) {
        return CAPABILITIES.get(player.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CAPABILITIES.remove(event.getEntity().getUUID());
    }

    public record ClientCapabilities(int protocolVersion, boolean hasChloride,
                                     boolean chlorideVisibility, Map<Integer, Boolean> visibility) {
        public ClientCapabilities(int protocolVersion, boolean hasChloride, boolean chlorideVisibility) {
            this(protocolVersion, hasChloride, chlorideVisibility, new HashMap<>());
        }
    }

    public record ClientCapabilitiesPacket(int protocolVersion, boolean hasChloride,
                                           boolean chlorideVisibility) {
        private static void encode(ClientCapabilitiesPacket packet, net.minecraft.network.FriendlyByteBuf buf) {
            buf.writeVarInt(packet.protocolVersion);
            buf.writeBoolean(packet.hasChloride);
            buf.writeBoolean(packet.chlorideVisibility);
        }

        private static ClientCapabilitiesPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
            return new ClientCapabilitiesPacket(buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
        }

        private static void handle(ClientCapabilitiesPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender != null && packet.protocolVersion == CLIENT_PROTOCOL_VERSION) {
                    recordCapabilities(sender, new ClientCapabilities(packet.protocolVersion,
                            packet.hasChloride, packet.chlorideVisibility));
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record EntityVisibilityBatchPacket(long[] states) {
        private static final int MAX_ENTRIES = 128;

        private static void encode(EntityVisibilityBatchPacket packet, net.minecraft.network.FriendlyByteBuf buf) {
            buf.writeVarInt(packet.states.length);
            for (long state : packet.states) {
                buf.writeVarLong(state);
            }
        }

        private static EntityVisibilityBatchPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            if (size < 0 || size > MAX_ENTRIES) {
                throw new IllegalArgumentException("Invalid entity visibility batch size: " + size);
            }
            long[] states = new long[size];
            for (int index = 0; index < size; index++) {
                states[index] = buf.readVarLong();
            }
            return new EntityVisibilityBatchPacket(states);
        }

        private static void handle(EntityVisibilityBatchPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null) {
                    return;
                }
                for (long state : packet.states) {
                    int entityId = (int) (state >>> 1);
                    if (entityId >= 0) {
                        recordVisibility(sender, entityId, (state & 1L) != 0L);
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }
}
