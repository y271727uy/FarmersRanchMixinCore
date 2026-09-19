package com.y271727uy.FRMC.capability.downland.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.y271727uy.FRMC.capability.downland.ContentPackRegistry;
import com.y271727uy.FRMC.capability.downland.fetch.DlcDownloader;
import com.y271727uy.FRMC.capability.downland.install.DlcUnpacker;
import com.y271727uy.FRMC.capability.downland.scan.DownloadPackEntry;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringUtil;
import net.minecraftforge.client.gui.widget.ScrollPanel;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.Size2i;
import net.minecraftforge.fml.loading.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AdditionalContentPackScreen extends Screen {
    private static final int PADDING = 6;
    private static final int BUTTON_MARGIN = 1;
    private static final int NUM_BUTTONS = SortType.values().length;

    private final Screen parentScreen;
    private final List<ContentPack> unsortedPacks;
    private final boolean showDownload;

    private ContentPackListWidget packList;
    private InfoPanel packInfo;
    private ContentPackListWidget.Entry selected;
    private int listWidth;
    private List<ContentPack> packs;
    private Button downloadButton;
    private Button installButton;
    private Button doneButton;
    private EditBox search;
    private String lastFilterText = "";
    private boolean sorted;
    private SortType sortType = SortType.NORMAL;
    private Component installMessage;
    private CompletableFuture<DlcDownloader.Result> pendingDownload;
    private String downloadingPackId;

    public AdditionalContentPackScreen(Screen parentScreen) {
        this(
                parentScreen,
                Component.translatable("frmc.additional_content_pack.title"),
                ContentPackCatalogItems.fromEntries(ContentPackRegistry.reload()),
                true
        );
    }

    AdditionalContentPackScreen(Screen parentScreen, Component title, List<ContentPack> packs, boolean showDownload) {
        super(title);
        this.parentScreen = parentScreen;
        this.showDownload = showDownload;
        this.unsortedPacks = List.copyOf(packs);
        this.packs = new ArrayList<>(this.unsortedPacks);
    }

    @Override
    protected void init() {
        listWidth = 0;
        for (ContentPack pack : packs) {
            listWidth = Math.max(listWidth, getFontRenderer().width(pack.displayName()) + 10);
            listWidth = Math.max(listWidth, getFontRenderer().width(listSubtitle(pack)) + 5);
            if (!pack.fileName().isEmpty()) {
                listWidth = Math.max(listWidth, getFontRenderer().width(pack.fileName()) + 10);
            }
        }
        listWidth = Math.max(Math.min(listWidth, width / 3), 100);
        listWidth += listWidth % NUM_BUTTONS != 0 ? (NUM_BUTTONS - listWidth % NUM_BUTTONS) : 0;

        int infoWidth = this.width - this.listWidth - (PADDING * 3);
        int doneButtonWidth = Math.min(infoWidth, 200);
        int y = this.height - 20 - PADDING;
        int fullButtonHeight = PADDING + 20 + PADDING;

        doneButton = Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds((listWidth + PADDING + this.width - doneButtonWidth) / 2, y, doneButtonWidth, 20)
                .build();
        downloadButton = Button.builder(Component.translatable("frmc.additional_content_pack.download"), button -> openSelectedDownload())
                .bounds(6, y, this.listWidth, 20)
                .build();
        installButton = Button.builder(Component.translatable("frmc.additional_content_pack.install"), button -> installSelected())
                .bounds(6, y, this.listWidth, 20)
                .build();

        y -= 14 + PADDING;
        search = new EditBox(getFontRenderer(), PADDING + 1, y, listWidth - 2, 14,
                Component.translatable("fml.menu.mods.search"));

        this.packList = new ContentPackListWidget(
                this,
                listWidth,
                fullButtonHeight,
                search.getY() - getFontRenderer().lineHeight - PADDING
        );
        this.packList.setLeftPos(6);
        this.packInfo = new InfoPanel(this.minecraft, infoWidth, this.height - PADDING - fullButtonHeight, PADDING);

        this.addRenderableWidget(packList);
        this.addRenderableWidget(packInfo);
        this.addRenderableWidget(search);
        this.addRenderableWidget(doneButton);
        if (showDownload) {
            this.addRenderableWidget(downloadButton);
        } else {
            this.addRenderableWidget(installButton);
        }

        search.setFocused(false);
        search.setCanLoseFocus(true);
        downloadButton.active = false;
        installButton.active = false;

        int sortWidth = listWidth / NUM_BUTTONS;
        int x = PADDING;
        addRenderableWidget(SortType.NORMAL.button = Button.builder(SortType.NORMAL.getButtonText(), button -> resortPacks(SortType.NORMAL))
                .bounds(x, PADDING, sortWidth - BUTTON_MARGIN, 20)
                .build());
        x += sortWidth + BUTTON_MARGIN;
        addRenderableWidget(SortType.A_TO_Z.button = Button.builder(SortType.A_TO_Z.getButtonText(), button -> resortPacks(SortType.A_TO_Z))
                .bounds(x, PADDING, sortWidth - BUTTON_MARGIN, 20)
                .build());
        x += sortWidth + BUTTON_MARGIN;
        addRenderableWidget(SortType.Z_TO_A.button = Button.builder(SortType.Z_TO_A.getButtonText(), button -> resortPacks(SortType.Z_TO_A))
                .bounds(x, PADDING, sortWidth - BUTTON_MARGIN, 20)
                .build());

        resortPacks(SortType.NORMAL);
        updateCache();
    }

    @Override
    public void tick() {
        pollPendingDownload();
        search.tick();
        packList.setSelected(selected);

        if (!search.getValue().equals(lastFilterText)) {
            reloadPacks();
            sorted = false;
        }

        if (!sorted) {
            reloadPacks();
            packs.sort(sortType);
            packList.refreshList();
            if (selected != null) {
                selected = packList.children().stream()
                        .filter(entry -> entry.getPack() == selected.getPack())
                        .findFirst()
                        .orElse(null);
                updateCache();
            }
            sorted = true;
        }
    }

    public <T extends ObjectSelectionList.Entry<T>> void buildPackList(Consumer<T> consumer, Function<ContentPack, T> factory) {
        for (ContentPack pack : packs) {
            consumer.accept(factory.apply(pack));
        }
    }

    public void setSelected(ContentPackListWidget.Entry entry) {
        this.selected = entry == this.selected ? null : entry;
        this.installMessage = null;
        updateCache();
    }

    public Minecraft getMinecraftInstance() {
        return minecraft;
    }

    public Font getFontRenderer() {
        return font;
    }

    static String listSubtitle(ContentPack pack) {
        if (!pack.valid()) {
            return Component.translatable("frmc.additional_content_pack.invalid").getString();
        }
        return pack.version() == null ? "" : pack.version();
    }

    private void reloadPacks() {
        this.packs = this.unsortedPacks.stream()
                .filter(this::matchesSearch)
                .collect(Collectors.toList());
        lastFilterText = search.getValue();
    }

    private boolean matchesSearch(ContentPack pack) {
        String query = StringUtils.toLowerCase(search.getValue());
        if (query.isEmpty()) {
            return true;
        }
        return contains(pack.displayName(), query)
                || contains(pack.fileName(), query)
                || contains(pack.id(), query)
                || contains(pack.version(), query);
    }

    private static boolean contains(String value, String query) {
        return StringUtils.toLowerCase(StringUtil.stripColor(value == null ? "" : value)).contains(query);
    }

    private void resortPacks(SortType newSort) {
        this.sortType = newSort;
        for (SortType sort : SortType.values()) {
            if (sort.button != null) {
                sort.button.active = sortType != sort;
            }
        }
        sorted = false;
    }

    private void openSelectedDownload() {
        if (!showDownload || selected == null || !selected.getPack().hasDownload()) {
            return;
        }
        this.minecraft.setScreen(selected.getPack().openDownload(this));
    }

    private void installSelected() {
        if (showDownload || selected == null) {
            return;
        }
        ContentPack pack = selected.getPack();
        if (!pack.valid() || ContentPackRegistry.isDownloading(pack.id())) {
            return;
        }
        DownloadPackEntry entry = ContentPackRegistry.findByFileName(pack.fileName());
        ContentPackRegistry.InstallJob job = ContentPackRegistry.install(entry);
        this.installMessage = unpackMessage(job.unpack());
        this.pendingDownload = job.downloads();
        this.downloadingPackId = this.pendingDownload == null ? null : pack.id();
        if (this.pendingDownload != null) {
            if (this.pendingDownload.isDone()) {
                applyDownloadResult(this.pendingDownload);
                this.pendingDownload = null;
                this.downloadingPackId = null;
            } else {
                this.installMessage = Component.translatable("frmc.additional_content_pack.download.started");
            }
        }
        updateCache();
    }

    private void pollPendingDownload() {
        if (pendingDownload == null || !pendingDownload.isDone()) {
            return;
        }
        String finishedId = downloadingPackId;
        CompletableFuture<DlcDownloader.Result> future = pendingDownload;
        pendingDownload = null;
        downloadingPackId = null;
        if (selected == null || !java.util.Objects.equals(selected.getPack().id(), finishedId)) {
            return;
        }
        applyDownloadResult(future);
        updateCache();
    }

    private void applyDownloadResult(CompletableFuture<DlcDownloader.Result> future) {
        try {
            this.installMessage = downloadMessage(future.join());
        } catch (CompletionException | CancellationException exception) {
            this.installMessage = Component.translatable("frmc.additional_content_pack.download.failed", 1);
        }
    }

    private static Component unpackMessage(DlcUnpacker.Result result) {
        return switch (result.status()) {
            case SUCCESS -> Component.translatable("frmc.additional_content_pack.install.ok");
            case ALREADY_INSTALLED -> Component.translatable("frmc.additional_content_pack.install.already");
            case REJECTED, FAILED -> Component.translatable("frmc.additional_content_pack.install.failed");
        };
    }

    private static Component downloadMessage(DlcDownloader.Result result) {
        if (result == null || result.failed() > 0) {
            int failed = result == null ? 1 : result.failed();
            return Component.translatable("frmc.additional_content_pack.download.failed", failed);
        }
        return Component.translatable("frmc.additional_content_pack.download.ok");
    }

    private void updateCache() {
        if (selected == null) {
            this.downloadButton.active = false;
            this.installButton.active = false;
            if (showDownload && unsortedPacks.isEmpty()) {
                packInfo.setInfo(List.of(Component.translatable("frmc.additional_content_pack.empty").getString()), null, new Size2i(0, 0));
            } else if (showDownload) {
                packInfo.setInfo(List.of(Component.translatable("frmc.additional_content_pack.select_hint").getString()), null, new Size2i(0, 0));
            } else {
                this.packInfo.clearInfo();
            }
            return;
        }

        ContentPack pack = selected.getPack();
        this.downloadButton.active = showDownload && pack.hasDownload();
        boolean downloading = ContentPackRegistry.isDownloading(pack.id())
                || (pendingDownload != null && !pendingDownload.isDone());
        this.installButton.active = !showDownload && pack.valid() && !downloading;
        if (installMessage == null && downloading) {
            this.installMessage = Component.translatable("frmc.additional_content_pack.download.started");
        }

        List<String> lines = new ArrayList<>();
        if (installMessage != null) {
            lines.add(installMessage.getString());
            lines.add(null);
        }
        lines.add(pack.displayName());
        if (!pack.fileName().isBlank()) {
            lines.add(Component.translatable("frmc.additional_content_pack.info.file", pack.fileName()).getString());
        }
        if (pack.valid()) {
            lines.add(Component.translatable("frmc.additional_content_pack.info.status.valid").getString());
        } else {
            Component reason = pack.issue() == null
                    ? Component.translatable("frmc.additional_content_pack.invalid")
                    : Component.translatable(pack.issue().translationKey());
            lines.add(Component.translatable("frmc.additional_content_pack.info.status.invalid", reason).getString());
        }
        if (!pack.version().isBlank()) {
            lines.add(Component.translatable("frmc.additional_content_pack.info.version", pack.version()).getString());
        }
        if (!pack.id().isBlank()) {
            lines.add(Component.translatable("frmc.additional_content_pack.info.id", pack.id()).getString());
        }
        if (!pack.authors().isBlank()) {
            lines.add(Component.translatable("frmc.additional_content_pack.info.authors", pack.authors()).getString());
        }
        if (!pack.license().isBlank()) {
            lines.add(Component.translatable("frmc.additional_content_pack.info.license", pack.license()).getString());
        }
        if (!pack.modpackVersion().isBlank()) {
            lines.add(Component.translatable("frmc.additional_content_pack.info.modpack_version", pack.modpackVersion()).getString());
        }
        if (!pack.dlcType().isBlank()) {
            lines.add(Component.translatable("frmc.additional_content_pack.info.dlc_type", pack.dlcType()).getString());
        }
        if (!pack.description().isBlank()) {
            lines.add(null);
            lines.add(pack.description());
        }

        boolean hasLogo = pack.logo() != null && pack.logoWidth() > 0 && pack.logoHeight() > 0;
        Size2i logoDims = hasLogo ? new Size2i(pack.logoWidth(), pack.logoHeight()) : new Size2i(0, 0);
        packInfo.setInfo(lines, hasLogo ? pack.logo() : null, logoDims);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.packList.render(graphics, mouseX, mouseY, partialTick);
        if (this.packInfo != null) {
            this.packInfo.render(graphics, mouseX, mouseY, partialTick);
        }

        Component text = Component.translatable("fml.menu.mods.search");
        int x = packList.getLeft() + ((packList.getRight() - packList.getLeft()) / 2) - (getFontRenderer().width(text) / 2);
        this.search.render(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(getFontRenderer(), text.getVisualOrderText(), x, search.getY() - getFontRenderer().lineHeight, 0xFFFFFF, false);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String value = this.search.getValue();
        SortType sort = this.sortType;
        ContentPackListWidget.Entry selectedEntry = this.selected;
        this.init(minecraft, width, height);
        this.search.setValue(value);
        this.selected = selectedEntry;
        if (!this.search.getValue().isEmpty()) {
            reloadPacks();
        }
        if (sort != SortType.NORMAL) {
            resortPacks(sort);
        }
        updateCache();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parentScreen);
    }

    private enum SortType implements Comparator<ContentPack> {
        NORMAL,
        A_TO_Z {
            @Override
            protected int compare(String name1, String name2) {
                return name1.compareTo(name2);
            }
        },
        Z_TO_A {
            @Override
            protected int compare(String name1, String name2) {
                return name2.compareTo(name1);
            }
        };

        Button button;

        protected int compare(String name1, String name2) {
            return 0;
        }

        @Override
        public int compare(ContentPack left, ContentPack right) {
            String name1 = StringUtils.toLowerCase(StringUtil.stripColor(left.displayName()));
            String name2 = StringUtils.toLowerCase(StringUtil.stripColor(right.displayName()));
            return compare(name1, name2);
        }

        Component getButtonText() {
            return Component.translatable("fml.menu.mods." + StringUtils.toLowerCase(name()));
        }
    }

    private final class InfoPanel extends ScrollPanel {
        private ResourceLocation logoPath;
        private Size2i logoDims = new Size2i(0, 0);
        private List<FormattedCharSequence> lines = Collections.emptyList();

        InfoPanel(Minecraft minecraft, int width, int height, int top) {
            super(minecraft, width, height, top, packList.getRight() + PADDING);
        }

        void setInfo(List<String> lines, ResourceLocation logoPath, Size2i logoDims) {
            this.logoPath = logoPath;
            this.logoDims = logoDims;
            this.lines = resizeContent(lines);
        }

        void clearInfo() {
            this.logoPath = null;
            this.logoDims = new Size2i(0, 0);
            this.lines = Collections.emptyList();
        }

        private List<FormattedCharSequence> resizeContent(List<String> lines) {
            List<FormattedCharSequence> result = new ArrayList<>();
            for (String line : lines) {
                if (line == null) {
                    result.add(null);
                    continue;
                }
                Component chat = ForgeHooks.newChatWithLinks(line, false);
                int maxTextLength = this.width - 12;
                if (maxTextLength >= 0) {
                    result.addAll(Language.getInstance().getVisualOrder(font.getSplitter().splitLines(chat, maxTextLength, Style.EMPTY)));
                }
            }
            return result;
        }

        @Override
        public int getContentHeight() {
            int height = logoPath == null ? 0 : 50;
            height += lines.size() * font.lineHeight;
            if (height < this.bottom - this.top - 8) {
                height = this.bottom - this.top - 8;
            }
            return height;
        }

        @Override
        protected int getScrollAmount() {
            return font.lineHeight * 3;
        }

        @Override
        protected void drawPanel(GuiGraphics graphics, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY) {
            if (logoPath != null) {
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                int headerHeight = 50;
                graphics.blitInscribed(
                        logoPath,
                        left + PADDING,
                        relativeY,
                        width - (PADDING * 2),
                        headerHeight,
                        logoDims.width,
                        logoDims.height,
                        false,
                        true
                );
                relativeY += headerHeight + PADDING;
            }

            for (FormattedCharSequence line : lines) {
                if (line != null) {
                    RenderSystem.enableBlend();
                    graphics.drawString(AdditionalContentPackScreen.this.font, line, left + PADDING, relativeY, 0xFFFFFF);
                    RenderSystem.disableBlend();
                }
                relativeY += font.lineHeight;
            }

            Style style = findTextLine(mouseX, mouseY);
            if (style != null) {
                graphics.renderComponentHoverEffect(AdditionalContentPackScreen.this.font, style, mouseX, mouseY);
            }
        }

        private Style findTextLine(int mouseX, int mouseY) {
            if (!isMouseOver(mouseX, mouseY)) {
                return null;
            }
            double offset = (mouseY - top - PADDING - border) + scrollDistance;
            if (logoPath != null) {
                offset -= 50;
            }
            if (offset <= 0) {
                return null;
            }
            int lineIdx = (int) (offset / font.lineHeight);
            if (lineIdx >= lines.size() || lineIdx < 0) {
                return null;
            }
            FormattedCharSequence line = lines.get(lineIdx);
            if (line != null) {
                return font.getSplitter().componentStyleAtWidth(line, mouseX - left - border);
            }
            return null;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            Style style = findTextLine((int) mouseX, (int) mouseY);
            if (style != null) {
                AdditionalContentPackScreen.this.handleComponentClicked(style);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(NarrationElementOutput output) {
        }
    }
}
