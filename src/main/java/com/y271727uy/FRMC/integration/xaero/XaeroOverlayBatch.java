package com.y271727uy.FRMC.integration.xaero;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.fml.ModList;

/** Keeps optional AR linkage out of Xaero's normal rendering path. */
final class XaeroOverlayBatch {
    private XaeroOverlayBatch() {
    }

    static int begin(GuiGraphics graphics) {
        if (!XaeroPerformanceConfig.batchOverlay() || !Availability.AR) {
            return 0;
        }
        return ArBridge.begin(graphics);
    }

    static void end(GuiGraphics graphics, int ownership) {
        if (ownership != 0) {
            ArBridge.end(graphics, ownership);
        }
    }

    private static final class Availability {
        private static final boolean AR = ModList.get().isLoaded("acceleratedrendering");
    }

    private static final class ArBridge {
        static int begin(GuiGraphics graphics) {
            if (!com.github.argon4w.acceleratedrendering.core.CoreFeature.isLoaded()
                    || com.github.argon4w.acceleratedrendering.core.CoreFeature.isGuiBatching()
                    || com.github.argon4w.acceleratedrendering.core.CoreFeature.shouldByPassGuiBatching()) {
                return 0;
            }
            boolean wasRenderingGui = com.github.argon4w.acceleratedrendering.core.CoreFeature.isRenderingGui();
            graphics.flush();
            return com.github.argon4w.acceleratedrendering.features.items.gui.GuiBatchingController.INSTANCE
                    .startBatching(graphics) ? (wasRenderingGui ? 3 : 1) : 0;
        }

        static void end(GuiGraphics graphics, int ownership) {
            try {
                com.github.argon4w.acceleratedrendering.features.items.gui.GuiBatchingController.INSTANCE
                        .flushBatching(graphics);
            } finally {
                com.github.argon4w.acceleratedrendering.core.CoreFeature.resetGuiBatching();
                if ((ownership & 2) != 0) {
                    com.github.argon4w.acceleratedrendering.core.CoreFeature.setRenderingGui();
                } else {
                    com.github.argon4w.acceleratedrendering.core.CoreFeature.resetRenderingGui();
                }
            }
        }
    }
}
