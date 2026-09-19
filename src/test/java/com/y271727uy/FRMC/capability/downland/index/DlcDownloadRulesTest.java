package com.y271727uy.FRMC.capability.downland.index;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DlcDownloadRulesTest {
    @Test
    void indexFileNameNeedsIndexAndJson() {
        assertTrue(DlcDownloadRules.isIndexFileName("index.json"));
        assertTrue(DlcDownloadRules.isIndexFileName("INDEX.JSON"));
        assertTrue(DlcDownloadRules.isIndexFileName("modrinth.index.json"));
        assertTrue(DlcDownloadRules.isIndexFileName("custom-index.json"));
        assertFalse(DlcDownloadRules.isIndexFileName("pack.json"));
        assertFalse(DlcDownloadRules.isIndexFileName("index.txt"));
        assertFalse(DlcDownloadRules.isIndexFileName("index"));
        assertFalse(DlcDownloadRules.isIndexFileName(".json"));
        assertFalse(DlcDownloadRules.isIndexFileName(""));
        assertFalse(DlcDownloadRules.isIndexFileName(null));
    }

    @Test
    void allowsOfficialCdnHostsOverHttps() {
        assertTrue(DlcDownloadRules.isAllowedDownloadUrl(
                "https://cdn.modrinth.com/data/DSVgwcji/versions/eJihmpNQ/AI-Improvements-1.20-0.5.2.jar"));
        assertTrue(DlcDownloadRules.isAllowedDownloadUrl(
                "https://edge.forgecdn.net/files/4578/262/AI-Improvements-1.20-0.5.2.jar"));
        assertTrue(DlcDownloadRules.isAllowedDownloadUrl(
                "https://mediafilez.forgecdn.net/files/4578/262/AI-Improvements-1.20-0.5.2.jar"));
        assertTrue(DlcDownloadRules.isAllowedDownloadUrl(
                "https://media.forgecdn.net/files/4578/262/AI-Improvements-1.20-0.5.2.jar"));
        assertTrue(DlcDownloadRules.isAllowedDownloadUrl(
                "HTTPS://CDN.MODRINTH.COM/data/x/file.jar"));
        assertTrue(DlcDownloadRules.isAllowedDownloadUrl(
                "https://cdn.modrinth.com:443/data/x/file.jar"));
    }

    @Test
    void rejectsNonWhitelistedUrls() {
        assertFalse(DlcDownloadRules.isAllowedDownloadUrl(
                "https://github.com/someone/repo/releases/download/v1/mod.jar"));
        assertFalse(DlcDownloadRules.isAllowedDownloadUrl(
                "https://www.curseforge.com/minecraft/mc-mods/example/download"));
        assertFalse(DlcDownloadRules.isAllowedDownloadUrl(
                "http://cdn.modrinth.com/data/x/file.jar"));
        assertFalse(DlcDownloadRules.isAllowedDownloadUrl(
                "https://user@cdn.modrinth.com/data/x/file.jar"));
        assertFalse(DlcDownloadRules.isAllowedDownloadUrl(
                "https://cdn.modrinth.com.evil.com/file.jar"));
        assertFalse(DlcDownloadRules.isAllowedDownloadUrl(
                "https://files.cdn.modrinth.com/file.jar"));
        assertFalse(DlcDownloadRules.isAllowedDownloadUrl(
                "https://1.2.3.4/file.jar"));
        assertFalse(DlcDownloadRules.isAllowedDownloadUrl(
                "https://[2001:db8::1]/file.jar"));
        assertFalse(DlcDownloadRules.isAllowedDownloadUrl(
                "https://cdn.modrinth.com:8443/file.jar"));
        assertFalse(DlcDownloadRules.isAllowedDownloadUrl("not a url"));
        assertFalse(DlcDownloadRules.isAllowedDownloadUrl(""));
        assertFalse(DlcDownloadRules.isAllowedDownloadUrl(null));
    }
}
