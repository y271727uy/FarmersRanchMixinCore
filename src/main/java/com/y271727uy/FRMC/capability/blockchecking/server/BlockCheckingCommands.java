package com.y271727uy.FRMC.capability.blockchecking.server;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.y271727uy.FRMC.config.BlockCheckingConfig;
import com.y271727uy.FRMC.capability.blockchecking.aggregate.BlockCheckingLimits;
import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureEntry;
import com.y271727uy.FRMC.capability.blockchecking.model.BlockCheckingReport;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkMetric;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** OP-only read-only block-checking commands. */
public final class BlockCheckingCommands {
    private BlockCheckingCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("frmc").requires(source -> source.hasPermission(2))
                .then(Commands.literal("blockchecking")
                        .then(Commands.literal("summary").executes(BlockCheckingCommands::summary))
                        .then(Commands.literal("chunks").executes(context -> chunks(context, BlockCheckingConfig.outputEntryLimit()))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(1, BlockCheckingLimits.MAX_OUTPUT_ENTRIES))
                                        .executes(context -> chunks(context, IntegerArgumentType.getInteger(context, "limit"))))))
                .then(Commands.literal("perf")
                        .then(Commands.literal("start").executes(context -> start(context, BlockCheckingConfig.defaultSessionSeconds()))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, BlockCheckingLimits.MAX_SESSION_SECONDS))
                                        .executes(context -> start(context, IntegerArgumentType.getInteger(context, "seconds")))))
                        .then(Commands.literal("stop").executes(BlockCheckingCommands::stop))
                        .then(Commands.literal("report").executes(BlockCheckingCommands::report))));
    }

    private static int summary(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Component status = !BlockCheckingConfig.enabled()
                ? Component.translatable("frmc.blockchecking.status.disabled")
                : BlockCheckingRuntime.active()
                ? Component.translatable("frmc.blockchecking.status.active")
                : Component.translatable("frmc.blockchecking.status.idle");
        source.sendSuccess(() -> Component.translatable("frmc.blockchecking.command.summary", status), false);
        source.sendSuccess(() -> Component.translatable("frmc.blockchecking.command.summary.scope"), false);
        return 1;
    }

    private static int chunks(CommandContext<CommandSourceStack> context, int requestedLimit) {
        int limit = Math.min(requestedLimit, BlockCheckingConfig.outputEntryLimit());
        var snapshot = BlockCheckingRuntime.lastChunks();
        context.getSource().sendSuccess(() -> Component.translatable(
                "frmc.blockchecking.command.chunks.header", snapshot.entries().size()), false);
        for (ChunkPressureEntry entry : snapshot.entries().stream().limit(limit).toList()) {
            context.getSource().sendSuccess(() -> Component.translatable(
                    "frmc.blockchecking.command.chunks.entry",
                    entry.dimension(),
                    entry.packedChunk(),
                    entry.trackedMobs(),
                    entry.targetModMobs(),
                    entry.otherHostileMobs(),
                    entry.riskScore()), false);
        }
        return 1;
    }

    private static int start(CommandContext<CommandSourceStack> context, int requestedSeconds) {
        if (!BlockCheckingConfig.enabled()) {
            context.getSource().sendFailure(Component.translatable("frmc.blockchecking.command.disabled"));
            return 0;
        }
        int seconds = Math.min(requestedSeconds, BlockCheckingConfig.maximumSessionSeconds());
        if (!BlockCheckingRuntime.start(seconds)) {
            context.getSource().sendFailure(Component.translatable("frmc.blockchecking.command.already_active"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("frmc.blockchecking.command.started", seconds), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        if (!BlockCheckingRuntime.stop()) {
            context.getSource().sendFailure(Component.translatable("frmc.blockchecking.command.not_active"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("frmc.blockchecking.command.stopped"), true);
        return 1;
    }

    private static int report(CommandContext<CommandSourceStack> context) {
        BlockCheckingReport report = BlockCheckingRuntime.lastReport();
        if (report == null) {
            context.getSource().sendFailure(Component.translatable("frmc.blockchecking.command.report.missing"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable(
                "frmc.blockchecking.command.report.header",
                report.durationNanos() / 1_000_000L,
                report.tickCount(),
                report.retainedSamples(),
                report.discardedSamples()), false);
        for (WorkMetric metric : report.metrics()) {
            context.getSource().sendSuccess(() -> Component.translatable(
                    "frmc.blockchecking.command.report.metric",
                    metric.kind().name(),
                    metric.samples(),
                    metric.averageNanos() / 1_000_000.0,
                    metric.maximumNanos() / 1_000_000.0), false);
        }
        context.getSource().sendSuccess(() -> Component.translatable("frmc.blockchecking.command.report.scope"), false);
        return 1;
    }
}
