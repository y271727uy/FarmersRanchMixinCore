package com.y271727uy.FRMC.network;

import com.y271727uy.FRMC.capability.blockchecking.aggregate.BlockCheckingLimits;
import com.y271727uy.FRMC.capability.blockchecking.client.ClientBlockCheckingNetworkHandler;
import com.y271727uy.FRMC.capability.blockchecking.server.BlockCheckingRuntime;
import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureEntry;
import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureSnapshot;
import com.y271727uy.FRMC.capability.blockchecking.model.BlockCheckingReport;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkKind;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkMetric;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.DistExecutor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class BlockCheckingNetwork {
    public static final int PROTOCOL_VERSION = 9;
    private static final String PROTOCOL = Integer.toString(PROTOCOL_VERSION);
    private static final int MAX_ENTRIES = BlockCheckingLimits.MAX_OUTPUT_ENTRIES;
    private static final int MAX_DIMENSION_LENGTH = 128;
    private static final int MAX_STRING_BYTES = 256;
    private static final int MAX_COUNT = BlockCheckingLimits.MAX_SAMPLES;
    private static final int MAX_TICKS = 1_000_000;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("frmc", "blockchecking"), () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL), NetworkRegistry.acceptMissingOr(PROTOCOL));
    private static final Set<UUID> REPORT_SUBSCRIBERS = new HashSet<>();
    private static final Set<UUID> CHUNK_VIEWERS = new HashSet<>();
    private static final AtomicLong REVISION = new AtomicLong();
    private static int nextId;

    private BlockCheckingNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(nextId++, SubscriptionRequestPacket.class,
                SubscriptionRequestPacket::encode, SubscriptionRequestPacket::decode,
                SubscriptionRequestPacket::handle);
        CHANNEL.registerMessage(nextId++, AnalysisRequestPacket.class,
                AnalysisRequestPacket::encode, AnalysisRequestPacket::decode,
                AnalysisRequestPacket::handle);
        CHANNEL.registerMessage(nextId++, ChunkSnapshotPacket.class,
                ChunkSnapshotPacket::encode, ChunkSnapshotPacket::decode, ChunkSnapshotPacket::handle);
        CHANNEL.registerMessage(nextId++, BlockCheckingReportPacket.class,
                BlockCheckingReportPacket::encode, BlockCheckingReportPacket::decode, BlockCheckingReportPacket::handle);
        CHANNEL.registerMessage(nextId++, InspectChunkRequestPacket.class,
                InspectChunkRequestPacket::encode, InspectChunkRequestPacket::decode,
                InspectChunkRequestPacket::handle);
        CHANNEL.registerMessage(nextId++, UninspectChunkRequestPacket.class,
                UninspectChunkRequestPacket::encode, UninspectChunkRequestPacket::decode,
                UninspectChunkRequestPacket::handle);
        CHANNEL.registerMessage(nextId++, ChunkSubscriptionRequestPacket.class,
                ChunkSubscriptionRequestPacket::encode, ChunkSubscriptionRequestPacket::decode,
                ChunkSubscriptionRequestPacket::handle);
    }

    public static void requestSubscription() {
        CHANNEL.sendToServer(new SubscriptionRequestPacket(true));
    }

    public static void requestChunkSubscription(boolean subscribe) {
        CHANNEL.sendToServer(new ChunkSubscriptionRequestPacket(subscribe));
    }

    public static void requestAnalysis(boolean enabled) {
        CHANNEL.sendToServer(new AnalysisRequestPacket(enabled));
    }

    public static void requestInspectChunk(String dimension, long packedChunk) {
        CHANNEL.sendToServer(new InspectChunkRequestPacket(dimension, packedChunk));
    }

    public static void requestUninspectChunk(String dimension, long packedChunk) {
        CHANNEL.sendToServer(new UninspectChunkRequestPacket(dimension, packedChunk));
    }

    public static void publishSnapshot(ChunkPressureSnapshot snapshot) {
        long revision = REVISION.incrementAndGet();
        for (UUID uuid : chunkViewersCopy()) {
            sendTo(uuid, new ChunkSnapshotPacket(revision, snapshot.capturedAtMillis(), snapshot));
        }
    }

    public static boolean hasChunkViewers() {
        synchronized (CHUNK_VIEWERS) {
            return !CHUNK_VIEWERS.isEmpty();
        }
    }

    public static void publishReport(BlockCheckingReport report) {
        if (report == null) return;
        long revision = REVISION.incrementAndGet();
        long timestamp = System.currentTimeMillis();
        for (UUID uuid : reportSubscribersCopy()) {
            sendTo(uuid, new BlockCheckingReportPacket(revision, timestamp, report));
        }
    }

    public static void clearSubscriptions() {
        synchronized (REPORT_SUBSCRIBERS) {
            REPORT_SUBSCRIBERS.clear();
        }
        synchronized (CHUNK_VIEWERS) {
            CHUNK_VIEWERS.clear();
        }
    }

    private static Set<UUID> reportSubscribersCopy() {
        synchronized (REPORT_SUBSCRIBERS) {
            return Set.copyOf(REPORT_SUBSCRIBERS);
        }
    }

    private static Set<UUID> chunkViewersCopy() {
        synchronized (CHUNK_VIEWERS) {
            return Set.copyOf(CHUNK_VIEWERS);
        }
    }

    private static void sendTo(UUID uuid, Object packet) {
        // Resolve the player only on the server event thread when the packet is sent.
        net.minecraft.server.MinecraftServer server = BlockCheckingRuntime.server();
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    private static void sendCurrentReport(ServerPlayer player) {
        BlockCheckingReport report = BlockCheckingRuntime.lastReport();
        if (report != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new BlockCheckingReportPacket(REVISION.incrementAndGet(), System.currentTimeMillis(), report));
        }
    }

    private static void sendCurrentChunks(ServerPlayer player) {
        ChunkPressureSnapshot snapshot = BlockCheckingRuntime.ensureChunkSnapshot();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ChunkSnapshotPacket(REVISION.incrementAndGet(), snapshot.capturedAtMillis(), snapshot));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        synchronized (REPORT_SUBSCRIBERS) {
            REPORT_SUBSCRIBERS.remove(uuid);
        }
        synchronized (CHUNK_VIEWERS) {
            CHUNK_VIEWERS.remove(uuid);
        }
    }

    public record SubscriptionRequestPacket(boolean subscribe) {
        private static void encode(SubscriptionRequestPacket packet, FriendlyByteBuf buf) { buf.writeBoolean(packet.subscribe); }
        private static SubscriptionRequestPacket decode(FriendlyByteBuf buf) { return new SubscriptionRequestPacket(buf.readBoolean()); }
        private static void handle(SubscriptionRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null || !sender.hasPermissions(2)) return;
                synchronized (REPORT_SUBSCRIBERS) {
                    if (packet.subscribe) REPORT_SUBSCRIBERS.add(sender.getUUID());
                    else REPORT_SUBSCRIBERS.remove(sender.getUUID());
                }
                if (packet.subscribe) sendCurrentReport(sender);
            });
            context.setPacketHandled(true);
        }
    }

    public record ChunkSubscriptionRequestPacket(boolean subscribe) {
        private static void encode(ChunkSubscriptionRequestPacket packet, FriendlyByteBuf buf) {
            buf.writeBoolean(packet.subscribe);
        }

        private static ChunkSubscriptionRequestPacket decode(FriendlyByteBuf buf) {
            return new ChunkSubscriptionRequestPacket(buf.readBoolean());
        }

        private static void handle(ChunkSubscriptionRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null || !sender.hasPermissions(2)) {
                    return;
                }
                synchronized (CHUNK_VIEWERS) {
                    if (packet.subscribe) {
                        CHUNK_VIEWERS.add(sender.getUUID());
                    } else {
                        BlockCheckingRuntime.clearChunkView();
                        CHUNK_VIEWERS.remove(sender.getUUID());
                    }
                }
                if (packet.subscribe) {
                    BlockCheckingRuntime.startChunkView(sender);
                    sendCurrentChunks(sender);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record AnalysisRequestPacket(boolean enabled) {
        private static void encode(AnalysisRequestPacket packet, FriendlyByteBuf buf) {
            buf.writeBoolean(packet.enabled);
        }

        private static AnalysisRequestPacket decode(FriendlyByteBuf buf) {
            return new AnalysisRequestPacket(buf.readBoolean());
        }

        private static void handle(AnalysisRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null || !sender.hasPermissions(2)) {
                    return;
                }
                if (packet.enabled) {
                    synchronized (REPORT_SUBSCRIBERS) {
                        REPORT_SUBSCRIBERS.add(sender.getUUID());
                    }
                    BlockCheckingRuntime.startFor(sender);
                } else {
                    synchronized (REPORT_SUBSCRIBERS) {
                        REPORT_SUBSCRIBERS.remove(sender.getUUID());
                    }
                    BlockCheckingRuntime.stop();
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record InspectChunkRequestPacket(String dimension, long packedChunk) {
        private static void encode(InspectChunkRequestPacket packet, FriendlyByteBuf buf) {
            writeString(buf, packet.dimension, MAX_DIMENSION_LENGTH);
            buf.writeLong(packet.packedChunk);
        }

        private static InspectChunkRequestPacket decode(FriendlyByteBuf buf) {
            return new InspectChunkRequestPacket(readString(buf, MAX_DIMENSION_LENGTH), buf.readLong());
        }

        private static void handle(InspectChunkRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null || !sender.hasPermissions(2)) {
                    return;
                }
                synchronized (CHUNK_VIEWERS) {
                    CHUNK_VIEWERS.add(sender.getUUID());
                }
                BlockCheckingRuntime.inspectChunk(
                        sender, packet.dimension, packet.packedChunk);
            });
            context.setPacketHandled(true);
        }
    }

    public record UninspectChunkRequestPacket(String dimension, long packedChunk) {
        private static void encode(UninspectChunkRequestPacket packet, FriendlyByteBuf buf) {
            writeString(buf, packet.dimension, MAX_DIMENSION_LENGTH);
            buf.writeLong(packet.packedChunk);
        }

        private static UninspectChunkRequestPacket decode(FriendlyByteBuf buf) {
            return new UninspectChunkRequestPacket(readString(buf, MAX_DIMENSION_LENGTH), buf.readLong());
        }

        private static void handle(UninspectChunkRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null || !sender.hasPermissions(2)) {
                    return;
                }
                synchronized (CHUNK_VIEWERS) {
                    CHUNK_VIEWERS.add(sender.getUUID());
                }
                BlockCheckingRuntime.uninspectChunk(
                        sender, packet.dimension, packet.packedChunk);
            });
            context.setPacketHandled(true);
        }
    }

    public record ChunkSnapshotPacket(long revision, long timestamp, ChunkPressureSnapshot snapshot) {
        private static void encode(ChunkSnapshotPacket packet, FriendlyByteBuf buf) {
            buf.writeLong(packet.revision); buf.writeLong(packet.timestamp);
            List<ChunkPressureEntry> entries = packet.snapshot.entries();
            buf.writeVarInt(entries.size());
            for (ChunkPressureEntry entry : entries) {
                writeString(buf, entry.dimension(), MAX_DIMENSION_LENGTH);
                buf.writeLong(entry.packedChunk()); buf.writeVarInt(entry.trackedMobs());
                buf.writeVarInt(entry.targetModMobs()); buf.writeVarInt(entry.otherHostileMobs());
                buf.writeVarInt(entry.tickingBlockEntities());
                buf.writeVarInt(entry.sleepingMobs());
                buf.writeVarInt(entry.throttledMobs());
                buf.writeVarInt(entry.lightingTasks());
                buf.writeVarInt(entry.itemEntities());
                buf.writeVarInt(entry.scheduledTicks());
                buf.writeVarInt(entry.nonMobEntities());
                buf.writeVarInt(entry.pathfindingMobs());
                buf.writeVarInt(entry.pendingThrottleMobs());
                buf.writeVarInt(entry.pendingSleepMobs());
            }
            buf.writeBoolean(packet.snapshot.trackedMobCoverage());
        }
        private static ChunkSnapshotPacket decode(FriendlyByteBuf buf) {
            long revision = boundedLong(buf.readLong()), timestamp = boundedLong(buf.readLong());
            int size = boundedSize(buf.readVarInt(), MAX_ENTRIES);
            List<ChunkPressureEntry> entries = new java.util.ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                String dimension = readString(buf, MAX_DIMENSION_LENGTH);
                entries.add(new ChunkPressureEntry(dimension, buf.readLong(), boundedCount(buf.readVarInt()),
                        boundedCount(buf.readVarInt()), boundedCount(buf.readVarInt()),
                        boundedCount(buf.readVarInt()), boundedCount(buf.readVarInt()),
                        boundedCount(buf.readVarInt()), boundedCount(buf.readVarInt()),
                        boundedCount(buf.readVarInt()), boundedCount(buf.readVarInt()),
                        boundedCount(buf.readVarInt()), boundedCount(buf.readVarInt()),
                        boundedCount(buf.readVarInt()), boundedCount(buf.readVarInt())));
            }
            return new ChunkSnapshotPacket(revision, timestamp,
                    new ChunkPressureSnapshot(timestamp, entries, buf.readBoolean()));
        }
        private static void handle(ChunkSnapshotPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    ClientBlockCheckingNetworkHandler.handle(packet)));
            context.setPacketHandled(true);
        }
    }

    public record BlockCheckingReportPacket(long revision, long timestamp, BlockCheckingReport report) {
        private static void encode(BlockCheckingReportPacket packet, FriendlyByteBuf buf) {
            buf.writeLong(packet.revision); buf.writeLong(packet.timestamp);
            BlockCheckingReport report = packet.report;
            buf.writeLong(report.startedAtNanos()); buf.writeLong(report.finishedAtNanos());
            buf.writeVarInt(report.tickCount()); buf.writeVarInt(report.retainedSamples()); buf.writeVarInt(report.discardedSamples());
            buf.writeVarInt(report.metrics().size());
            for (WorkMetric metric : report.metrics()) {
                buf.writeVarInt(metric.kind().ordinal()); buf.writeVarInt(metric.samples());
                buf.writeLong(metric.totalNanos()); buf.writeLong(metric.maximumNanos());
            }
        }
        private static BlockCheckingReportPacket decode(FriendlyByteBuf buf) {
            long revision = boundedLong(buf.readLong()), timestamp = boundedLong(buf.readLong());
            long started = boundedLong(buf.readLong()), finished = boundedLong(buf.readLong());
            if (finished < started) throw new IllegalArgumentException("Invalid report time range");
            int ticks = boundedValue(buf.readVarInt(), MAX_TICKS), retained = boundedCount(buf.readVarInt()), discarded = boundedCount(buf.readVarInt());
            int size = boundedSize(buf.readVarInt(), MAX_ENTRIES);
            List<WorkMetric> metrics = new java.util.ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int kind = buf.readVarInt();
                if (kind < 0 || kind >= WorkKind.values().length) throw new IllegalArgumentException("Invalid work kind");
                metrics.add(new WorkMetric(WorkKind.values()[kind], boundedCount(buf.readVarInt()),
                        boundedLong(buf.readLong()), boundedLong(buf.readLong())));
            }
            return new BlockCheckingReportPacket(revision, timestamp,
                    new BlockCheckingReport(started, finished, ticks, retained, discarded, metrics));
        }
        private static void handle(BlockCheckingReportPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    ClientBlockCheckingNetworkHandler.handle(packet)));
            context.setPacketHandled(true);
        }
    }

    private static int boundedSize(int value, int maximum) {
        if (value < 0 || value > maximum) throw new IllegalArgumentException("Invalid packet size: " + value);
        return value;
    }
    private static int boundedCount(int value) {
        if (value < 0 || value > MAX_COUNT) throw new IllegalArgumentException("Invalid packet count: " + value);
        return value;
    }
    private static int boundedValue(int value, int maximum) {
        if (value < 0 || value > maximum) throw new IllegalArgumentException("Invalid packet value: " + value);
        return value;
    }
    private static long boundedLong(long value) {
        if (value < 0) throw new IllegalArgumentException("Invalid packet value");
        return value;
    }
    private static void writeString(FriendlyByteBuf buf, String value, int maxChars) {
        if (value.length() > maxChars || value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_STRING_BYTES)
            throw new IllegalArgumentException("String exceeds packet limit");
        buf.writeUtf(value, maxChars);
    }
    private static String readString(FriendlyByteBuf buf, int maxChars) {
        String value = buf.readUtf(maxChars);
        if (value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_STRING_BYTES)
            throw new IllegalArgumentException("String exceeds packet limit");
        return value;
    }
}
