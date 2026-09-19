package com.y271727uy.FRMC.capability.downland.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.StringUtil;

public final class ContentPackListWidget extends ObjectSelectionList<ContentPackListWidget.Entry> {
    private static final int VALID_NAME_COLOR = 0xFFFFFF;
    private static final int INVALID_NAME_COLOR = 0xFF5555;
    private static final int VALID_VERSION_COLOR = 0xCCCCCC;
    private static final int INVALID_VERSION_COLOR = 0xAA5555;

    private final AdditionalContentPackScreen parent;
    private final int listWidth;

    public ContentPackListWidget(AdditionalContentPackScreen parent, int listWidth, int top, int bottom) {
        super(parent.getMinecraftInstance(), listWidth, parent.height, top, bottom, parent.getFontRenderer().lineHeight * 2 + 8);
        this.parent = parent;
        this.listWidth = listWidth;
        this.refreshList();
    }

    @Override
    protected int getScrollbarPosition() {
        return this.listWidth;
    }

    @Override
    public int getRowWidth() {
        return this.listWidth;
    }

    public void refreshList() {
        this.clearEntries();
        this.parent.buildPackList(this::addEntry, pack -> new Entry(pack, this.parent));
    }

    @Override
    protected void renderBackground(GuiGraphics graphics) {
        this.parent.renderBackground(graphics);
    }

    public final class Entry extends ObjectSelectionList.Entry<Entry> {
        private final ContentPack pack;
        private final AdditionalContentPackScreen parent;

        Entry(ContentPack pack, AdditionalContentPackScreen parent) {
            this.pack = pack;
            this.parent = parent;
        }

        public ContentPack getPack() {
            return pack;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", pack.displayName());
        }

        @Override
        public void render(
                GuiGraphics graphics,
                int index,
                int top,
                int left,
                int width,
                int height,
                int mouseX,
                int mouseY,
                boolean hovering,
                float partialTick
        ) {
            Font font = this.parent.getFontRenderer();
            Component name = Component.literal(StringUtil.stripColor(pack.displayName()));
            Component version = Component.literal(StringUtil.stripColor(AdditionalContentPackScreen.listSubtitle(pack)));
            graphics.drawString(
                    font,
                    Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(name, listWidth))),
                    left + 3,
                    top + 2,
                    pack.valid() ? VALID_NAME_COLOR : INVALID_NAME_COLOR,
                    false
            );
            graphics.drawString(
                    font,
                    Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(version, listWidth))),
                    left + 3,
                    top + 2 + font.lineHeight,
                    pack.valid() ? VALID_VERSION_COLOR : INVALID_VERSION_COLOR,
                    false
            );
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            parent.setSelected(this);
            ContentPackListWidget.this.setSelected(this);
            return true;
        }
    }
}
