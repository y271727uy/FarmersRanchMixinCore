package com.y271727uy.FRMC.capability.downland.ui;

import com.y271727uy.FRMC.capability.downland.scan.DownloadPackEntry;
import com.y271727uy.FRMC.capability.downland.scan.DownloadPackRules;
import com.y271727uy.FRMC.capability.downland.scan.ParsedDlc;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class ContentPackCatalogItems {
    private ContentPackCatalogItems() {}

    static List<ContentPack> fromEntries(List<DownloadPackEntry> entries) {
        List<ContentPack> packs = new ArrayList<>(entries.size());
        for (DownloadPackEntry entry : entries) {
            packs.add(fromEntry(entry));
        }
        return packs;
    }

    static ContentPack fromEntry(DownloadPackEntry entry) {
        if (!entry.valid() || entry.dlc() == null) {
            return invalidItem(entry);
        }
        ParsedDlc dlc = entry.dlc();
        ContentPack detail = detailItem(entry, dlc);
        return catalogItem(entry, dlc, parent -> new AdditionalContentPackScreen(
                parent,
                Component.translatable("frmc.additional_content_pack.detail.title"),
                List.of(detail),
                false
        ));
    }

    private static ContentPack catalogItem(DownloadPackEntry entry, ParsedDlc dlc, java.util.function.Function<Screen, Screen> download) {
        return new ContentPack(
                dlc.id(),
                dlc.displayName(),
                dlc.version(),
                dlc.authors(),
                dlc.description(),
                dlc.license(),
                dlc.modpackVersion(),
                dlc.dlcType(),
                null,
                0,
                0,
                download,
                true,
                null,
                entry.fileName()
        );
    }

    private static ContentPack detailItem(DownloadPackEntry entry, ParsedDlc dlc) {
        return new ContentPack(
                dlc.id(),
                dlc.displayName(),
                dlc.version(),
                dlc.authors(),
                dlc.description(),
                dlc.license(),
                dlc.modpackVersion(),
                dlc.dlcType(),
                null,
                0,
                0,
                null,
                true,
                null,
                entry.fileName()
        );
    }

    private static ContentPack invalidItem(DownloadPackEntry entry) {
        ParsedDlc dlc = entry.dlc();
        String fallbackName = displayStem(entry.fileName());
        return new ContentPack(
                dlc != null && !dlc.id().isEmpty() ? dlc.id() : fallbackName,
                dlc != null && !dlc.displayName().isBlank() ? dlc.displayName() : fallbackName,
                dlc != null ? dlc.version() : "",
                dlc != null ? dlc.authors() : "",
                dlc != null ? dlc.description() : "",
                dlc != null ? dlc.license() : "",
                dlc != null ? dlc.modpackVersion() : "",
                dlc != null ? dlc.dlcType() : "",
                null,
                0,
                0,
                null,
                false,
                entry.issue(),
                entry.fileName()
        );
    }

    static String displayStem(String fileName) {
        String stem = DownloadPackRules.zipStem(fileName);
        return stem.isEmpty() ? fileName : stem;
    }
}
