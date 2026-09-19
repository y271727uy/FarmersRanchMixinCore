package com.y271727uy.FRMC.capability.downland.scan;

public record ParsedDlc(
        String id,
        String displayName,
        String version,
        String authors,
        String description,
        String license,
        String modpackVersion,
        String dlcType,
        String icon
) {
    public ParsedDlc {
        id = id == null ? "" : id;
        displayName = displayName == null || displayName.isEmpty() ? id : displayName;
        version = version == null ? "" : version;
        authors = authors == null ? "" : authors;
        description = description == null ? "" : description;
        license = license == null ? "" : license;
        modpackVersion = modpackVersion == null ? "" : modpackVersion;
        dlcType = dlcType == null ? "" : dlcType;
        icon = icon == null ? "" : icon;
    }
}
