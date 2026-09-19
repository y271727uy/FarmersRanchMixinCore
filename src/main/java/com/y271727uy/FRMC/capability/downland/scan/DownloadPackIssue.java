package com.y271727uy.FRMC.capability.downland.scan;

public enum DownloadPackIssue {
    UNREADABLE("frmc.additional_content_pack.invalid.unreadable"),
    MISSING_PACK_TOML("frmc.additional_content_pack.invalid.missing_pack_toml"),
    PACK_TOML_TOO_LARGE("frmc.additional_content_pack.invalid.pack_toml_too_large"),
    PARSE_FAILED("frmc.additional_content_pack.invalid.parse_failed"),
    NO_DLC("frmc.additional_content_pack.invalid.no_dlc"),
    MULTIPLE_DLC("frmc.additional_content_pack.invalid.multiple_dlc"),
    INVALID_DLC_ID("frmc.additional_content_pack.invalid.invalid_dlc_id"),
    NAME_MISMATCH("frmc.additional_content_pack.invalid.name_mismatch");

    private final String translationKey;

    DownloadPackIssue(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
