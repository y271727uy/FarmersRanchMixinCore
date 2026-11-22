package com.y271727uy.FRMC.capability.villager.diagnostics;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.y271727uy.FRMC.capability.entity.manager.village.VillagerFreezeManager;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** OP-only {@code /frmc villager profile start|stop|report}. */
public final class VillagerProfilerCommands {
    private VillagerProfilerCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("frmc").requires(source -> source.hasPermission(2))
                .then(Commands.literal("villager")
                        .then(Commands.literal("freeze").executes(VillagerProfilerCommands::freezeStatus))
                        .then(Commands.literal("profile")
                                .then(Commands.literal("start")
                                        .executes(context -> start(context, VillagerProfiler.DEFAULT_SECONDS))
                                        .then(Commands.argument("seconds",
                                                        IntegerArgumentType.integer(1, VillagerProfiler.MAX_SECONDS))
                                                .executes(context -> start(context,
                                                        IntegerArgumentType.getInteger(context, "seconds")))))
                                .then(Commands.literal("stop").executes(VillagerProfilerCommands::stop))
                                .then(Commands.literal("report").executes(VillagerProfilerCommands::report)))));
    }

    static void send(CommandSourceStack source, VillagerProfileReport report) {
        for (VillagerProfileLines.Line line : VillagerProfileLines.of(report)) {
            source.sendSuccess(() -> Component.translatableWithFallback(line.key(), line.english(), line.args()), false);
        }
    }

    private static int freezeStatus(CommandContext<CommandSourceStack> context) {
        VillagerFreezeManager.Status status = VillagerFreezeManager.status();
        context.getSource().sendSuccess(() -> Component.translatable("frmc.villager.freeze.status",
                status.enabled(), String.format(Locale.ROOT, "%.1f", status.mspt()),
                String.format(Locale.ROOT, "%.0f", status.engageMspt()),
                String.format(Locale.ROOT, "%.0f", status.releaseMspt()),
                status.pressure(), status.frozen()), false);
        return 1;
    }

    private static int start(CommandContext<CommandSourceStack> context, int seconds) {
        if (!VillagerProfiler.start(seconds, context.getSource())) {
            context.getSource().sendFailure(Component.translatable("frmc.villager.profile.already_active"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("frmc.villager.profile.started", seconds), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        if (!VillagerProfiler.stop()) {
            context.getSource().sendFailure(Component.translatable("frmc.villager.profile.not_active"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("frmc.villager.profile.stopped"), true);
        return 1;
    }

    private static int report(CommandContext<CommandSourceStack> context) {
        VillagerProfileReport report = VillagerProfiler.lastReport();
        if (report == null) {
            context.getSource().sendFailure(Component.translatable("frmc.villager.profile.report.missing"));
            return 0;
        }
        send(context.getSource(), report);
        return 1;
    }
}
