package com.y271727uy.FRMC.integration.xaero;

import com.y271727uy.FRMC.integration.embeddium.EmbeddiumChunkRenderProbe;
import com.y271727uy.FRMC.integration.lumenized.LumenizedChunkLightProbe;
import com.y271727uy.FRMC.capability.blockchecking.aggregate.ChunkPressureIndex;
import com.y271727uy.FRMC.capability.blockchecking.client.ClientBlockCheckingNetworkHandler;
import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureEntry;
import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureSnapshot;
import com.y271727uy.FRMC.capability.blockchecking.model.BlockCheckingReport;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkKind;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkMetric;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import xaero.map.gui.GuiMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Full-screen World Map projection using its own camera and scale. */
final class XaeroWorldMapOverlay {
    private XaeroWorldMapOverlay() {
    }

    static void render(GuiGraphics graphics, GuiMap map, int mouseX, int mouseY,
            double cameraX, double cameraZ, double mapScale) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        boolean drewStatus = false;
        ChunkPressureSnapshot snapshot = ClientBlockCheckingNetworkHandler.STATE.snapshot();
        if (snapshot == null) {
            drawStatus(graphics, minecraft, Component.translatable("frmc.blockchecking.map.waiting"));
            drewStatus = true;
            drawSessionReport(graphics, minecraft, reportY(drewStatus));
            return;
        }
        if (snapshot.entries().isEmpty()) {
            drawStatus(graphics, minecraft, Component.translatable("frmc.blockchecking.map.empty"));
            drewStatus = true;
            drawSessionReport(graphics, minecraft, reportY(drewStatus));
            return;
        }
        Screen screen = map;
        String dimension = minecraft.level.dimension().location().toString();
        double pixelsPerBlock = worldMapPixelsPerBlock(mapScale);
        if (!(pixelsPerBlock > 0.0D) || !Double.isFinite(pixelsPerBlock)) {
            drawSessionReport(graphics, minecraft, reportY(false));
            return;
        }

        ChunkPressureEntry hovered = null;
        int visible = 0;
        LumenizedChunkLightProbe.refresh();
        graphics.enableScissor(0, 0, screen.width, screen.height);
        try {
            for (ChunkPressureEntry entry : snapshot.entries()) {
                if (!dimension.equals(entry.dimension())) {
                    continue;
                }
                int chunkX = ChunkPos.getX(entry.packedChunk());
                int chunkZ = ChunkPos.getZ(entry.packedChunk());
                double leftWorld = chunkX * 16.0D;
                double topWorld = chunkZ * 16.0D;
                double rightWorld = (chunkX + 1) * 16.0D;
                double bottomWorld = (chunkZ + 1) * 16.0D;
                int left = (int) Math.floor(screen.width * 0.5D
                        + (leftWorld - cameraX) * pixelsPerBlock);
                int top = (int) Math.floor(screen.height * 0.5D
                        + (topWorld - cameraZ) * pixelsPerBlock);
                int right = (int) Math.ceil(screen.width * 0.5D
                        + (rightWorld - cameraX) * pixelsPerBlock);
                int bottom = (int) Math.ceil(screen.height * 0.5D
                        + (bottomWorld - cameraZ) * pixelsPerBlock);
                if (right <= left || bottom <= top) {
                    continue;
                }
                visible++;
                long pressure = ChunkPressureIndex.score(entry, LumenizedChunkLightProbe.count(entry.packedChunk()));
                graphics.fill(left, top, right, bottom, ChunkPressureIndex.colorArgb(pressure));
                if (mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom) {
                    hovered = entry;
                }
            }
        } finally {
            graphics.disableScissor();
        }

        if (hovered != null) {
            drawHoverPanel(graphics, minecraft, screen, mouseX, mouseY, tooltip(hovered));
        } else if (visible == 0) {
            drawStatus(graphics, minecraft, Component.translatable("frmc.blockchecking.map.empty"));
            drewStatus = true;
        }
        drawSessionReport(graphics, minecraft, reportY(drewStatus));
    }

    private static int reportY(boolean drewStatus) {
        return drewStatus ? 56 + Minecraft.getInstance().font.lineHeight + 8 : 56;
    }

    private static void drawStatus(GuiGraphics graphics, Minecraft minecraft, Component text) {
        drawPanel(graphics, minecraft, 3, 56, List.of(text));
    }

    private static void drawSessionReport(GuiGraphics graphics, Minecraft minecraft, int y) {
        BlockCheckingReport report = ClientBlockCheckingNetworkHandler.STATE.report();
        if (report == null || report.metrics().isEmpty()) {
            return;
        }
        List<Component> lines = new ArrayList<>(3);
        appendMetric(lines, report, WorkKind.ENTITY_TICK, "frmc.blockchecking.map.report.entity");
        appendMetric(lines, report, WorkKind.CHUNK_TICK, "frmc.blockchecking.map.report.chunk");
        appendMetric(lines, report, WorkKind.BLOCK_ENTITY_TICK, "frmc.blockchecking.map.report.block_entity");
        if (lines.isEmpty()) {
            return;
        }
        drawPanel(graphics, minecraft, 3, y, lines);
    }

    private static void appendMetric(List<Component> lines, BlockCheckingReport report, WorkKind kind, String key) {
        for (WorkMetric metric : report.metrics()) {
            if (metric.kind() == kind && metric.samples() > 0) {
                lines.add(Component.translatable(key, String.format(Locale.ROOT, "%.1f",
                        metric.averageNanos() / 1_000_000.0D)));
                return;
            }
        }
    }

    private static void drawHoverPanel(GuiGraphics graphics, Minecraft minecraft, Screen screen,
            int mouseX, int mouseY, List<Component> lines) {
        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, minecraft.font.width(line));
        }
        int height = minecraft.font.lineHeight * lines.size();
        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + width + 3 > screen.width) {
            x = mouseX - width - 15;
        }
        x = Math.max(3, Math.min(x, screen.width - width - 3));
        if (y + height + 2 > screen.height) {
            y = screen.height - height - 4;
        }
        y = Math.max(2, y);
        drawPanel(graphics, minecraft, x, y, lines);
    }

    private static void drawPanel(GuiGraphics graphics, Minecraft minecraft, int x, int y, List<Component> lines) {
        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, minecraft.font.width(line));
        }
        int lineHeight = minecraft.font.lineHeight;
        int height = lineHeight * lines.size();
        graphics.fill(x - 3, y - 2, x + width + 3, y + height + 2, 0xC0000000);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(minecraft.font, lines.get(i), x, y + i * lineHeight, 0xFFFFFFFF, false);
        }
    }

    private static List<Component> tooltip(ChunkPressureEntry entry) {
        int chunkX = ChunkPos.getX(entry.packedChunk());
        int chunkZ = ChunkPos.getZ(entry.packedChunk());
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.chunk", chunkX, chunkZ));
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.mobs",
                entry.trackedMobs(), entry.targetModMobs()));
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.hostile", entry.otherHostileMobs()));
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.block_entities",
                entry.tickingBlockEntities()));
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.items", entry.itemEntities()));
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.non_mobs", entry.nonMobEntities()));
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.pathfinding", entry.pathfindingMobs()));
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.scheduled", entry.scheduledTicks()));
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.sleeping", entry.sleepingMobs()));
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.throttled", entry.throttledMobs()));
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.pending_throttled",
                entry.pendingThrottleMobs()));
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.pending_sleeping",
                entry.pendingSleepMobs()));
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.freeze_pressure",
                ChunkPressureIndex.freezeScore(entry)));
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.lighting", entry.lightingTasks()));
        int postLights = LumenizedChunkLightProbe.count(entry.packedChunk());
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.post_lights", postLights));
        lines.add(Component.translatable("frmc.blockchecking.map.tooltip.pressure",
                ChunkPressureIndex.score(entry, postLights)));
        EmbeddiumChunkRenderProbe.Probe render = EmbeddiumChunkRenderProbe.probe(chunkX, chunkZ);
        if (render != null) {
            lines.add(Component.translatable("frmc.blockchecking.map.tooltip.render",
                    render.rebuildSections(), render.geometrySections()));
        }
        return lines;
    }

    /**
     * Map camera scale is in framebuffer pixels per block. Overlay drawing
     * happens in GUI-scaled coordinates, so convert through the two window ratios.
     */
    private static double worldMapPixelsPerBlock(double mapCameraScale) {
        var window = Minecraft.getInstance().getWindow();
        double framebufferToGui = (double) window.getScreenWidth() / (double) window.getGuiScaledWidth();
        double windowToFramebuffer = (double) window.getWidth() / (double) window.getScreenWidth();
        return mapCameraScale / (framebufferToGui * windowToFramebuffer);
    }
}
