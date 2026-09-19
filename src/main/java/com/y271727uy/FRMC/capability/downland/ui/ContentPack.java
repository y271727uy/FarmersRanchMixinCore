package com.y271727uy.FRMC.capability.downland.ui;

import com.y271727uy.FRMC.capability.downland.scan.DownloadPackIssue;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public final class ContentPack {
    private final String id;
    private final String displayName;
    private final String version;
    private final String authors;
    private final String description;
    private final String license;
    private final String modpackVersion;
    private final String dlcType;
    private final ResourceLocation logo;
    private final int logoWidth;
    private final int logoHeight;
    private final Function<Screen, Screen> downloadScreenFactory;
    private final boolean valid;
    private final DownloadPackIssue issue;
    private final String fileName;

    public ContentPack(String id, String displayName, String version, String authors, String description) {
        this(id, displayName, version, authors, description, "", "", "", null, 0, 0, null, true, null, "");
    }

    public ContentPack(
            String id,
            String displayName,
            String version,
            String authors,
            String description,
            String license,
            String modpackVersion,
            String dlcType,
            ResourceLocation logo,
            int logoWidth,
            int logoHeight,
            Function<Screen, Screen> downloadScreenFactory
    ) {
        this(
                id,
                displayName,
                version,
                authors,
                description,
                license,
                modpackVersion,
                dlcType,
                logo,
                logoWidth,
                logoHeight,
                downloadScreenFactory,
                true,
                null,
                ""
        );
    }

    public ContentPack(
            String id,
            String displayName,
            String version,
            String authors,
            String description,
            String license,
            String modpackVersion,
            String dlcType,
            ResourceLocation logo,
            int logoWidth,
            int logoHeight,
            Function<Screen, Screen> downloadScreenFactory,
            boolean valid,
            DownloadPackIssue issue,
            String fileName
    ) {
        this.id = id;
        this.displayName = displayName;
        this.version = version;
        this.authors = authors == null ? "" : authors;
        this.description = description == null ? "" : description;
        this.license = license == null ? "" : license;
        this.modpackVersion = modpackVersion == null ? "" : modpackVersion;
        this.dlcType = dlcType == null ? "" : dlcType;
        this.logo = logo;
        this.logoWidth = logoWidth;
        this.logoHeight = logoHeight;
        this.downloadScreenFactory = downloadScreenFactory;
        this.valid = valid;
        this.issue = issue;
        this.fileName = fileName == null ? "" : fileName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String version() {
        return version;
    }

    public String authors() {
        return authors;
    }

    public String description() {
        return description;
    }

    public String license() {
        return license;
    }

    public String modpackVersion() {
        return modpackVersion;
    }

    public String dlcType() {
        return dlcType;
    }

    public ResourceLocation logo() {
        return logo;
    }

    public int logoWidth() {
        return logoWidth;
    }

    public int logoHeight() {
        return logoHeight;
    }

    public boolean valid() {
        return valid;
    }

    public DownloadPackIssue issue() {
        return issue;
    }

    public String fileName() {
        return fileName;
    }

    public boolean hasDownload() {
        return downloadScreenFactory != null;
    }

    public Screen openDownload(Screen parent) {
        return downloadScreenFactory.apply(parent);
    }
}
