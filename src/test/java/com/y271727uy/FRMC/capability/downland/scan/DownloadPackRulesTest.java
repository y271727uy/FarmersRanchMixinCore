package com.y271727uy.FRMC.capability.downland.scan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadPackRulesTest {
    @Test
    void zipStemStripsExtensionCaseInsensitively() {
        assertEquals("vietnam", DownloadPackRules.zipStem("vietnam.zip"));
        assertEquals("vietnam-1.0.0", DownloadPackRules.zipStem("vietnam-1.0.0.ZIP"));
        assertEquals("vietnam", DownloadPackRules.zipStem("vietnam.mrpack"));
        assertEquals("vietnam-1.0.0", DownloadPackRules.zipStem("vietnam-1.0.0.MRPACK"));
        assertEquals("", DownloadPackRules.zipStem("vietnam.txt"));
        assertEquals("", DownloadPackRules.zipStem(".zip"));
        assertEquals("", DownloadPackRules.zipStem(".mrpack"));
        assertEquals("", DownloadPackRules.zipStem("vietnam.zip.bak"));
    }

    @Test
    void onlyExactRootPackTomlCounts() {
        assertTrue(DownloadPackRules.isRootPackToml("pack.toml"));
        assertFalse(DownloadPackRules.isRootPackToml("Pack.toml"));
        assertFalse(DownloadPackRules.isRootPackToml("foo/pack.toml"));
        assertFalse(DownloadPackRules.isRootPackToml("./pack.toml"));
    }

    @Test
    void packIdRejectsBlankAndPathSegments() {
        assertTrue(DownloadPackRules.isValidPackId("vietnam"));
        assertFalse(DownloadPackRules.isValidPackId(""));
        assertFalse(DownloadPackRules.isValidPackId(" "));
        assertFalse(DownloadPackRules.isValidPackId("."));
        assertFalse(DownloadPackRules.isValidPackId(".."));
        assertFalse(DownloadPackRules.isValidPackId("foo/bar"));
        assertFalse(DownloadPackRules.isValidPackId("foo\\bar"));
    }

    @Test
    void zipNameMatchesIdOrIdDashVersion() {
        assertTrue(DownloadPackRules.matchesZipName("vietnam.zip", "vietnam", "1.0.0"));
        assertTrue(DownloadPackRules.matchesZipName("vietnam.zip", "vietnam", ""));
        assertTrue(DownloadPackRules.matchesZipName("vietnam-1.0.0.zip", "vietnam", "1.0.0"));
        assertTrue(DownloadPackRules.matchesZipName("vietnam-1.0.0.ZIP", "vietnam", "1.0.0"));
        assertTrue(DownloadPackRules.matchesZipName("vietnam.mrpack", "vietnam", "1.0.0"));
        assertTrue(DownloadPackRules.matchesZipName("vietnam.mrpack", "vietnam", ""));
        assertTrue(DownloadPackRules.matchesZipName("vietnam-1.0.0.mrpack", "vietnam", "1.0.0"));
        assertTrue(DownloadPackRules.matchesZipName("vietnam-1.0.0.MRPACK", "vietnam", "1.0.0"));
    }

    @Test
    void zipNameMismatchIsCaseSensitiveAndVersionExact() {
        assertFalse(DownloadPackRules.matchesZipName("Vietnam.zip", "vietnam", "1.0.0"));
        assertFalse(DownloadPackRules.matchesZipName("vietnam-1.0.0.zip", "vietnam", "1.1.0"));
        assertFalse(DownloadPackRules.matchesZipName("vietnam-1.0.0.zip", "vietnam", "1.0.0 "));
        assertFalse(DownloadPackRules.matchesZipName("vietnam-1.0.0.zip", "vietnam", ""));
        assertFalse(DownloadPackRules.matchesZipName("vietnam-1.0.0.zip", "other", "1.0.0"));
        assertFalse(DownloadPackRules.matchesZipName("vietnam.txt", "vietnam", "1.0.0"));
        assertFalse(DownloadPackRules.matchesZipName("Vietnam.mrpack", "vietnam", "1.0.0"));
        assertFalse(DownloadPackRules.matchesZipName("vietnam-1.0.0.mrpack", "vietnam", "1.1.0"));
        assertFalse(DownloadPackRules.matchesZipName("vietnam.mrpack.bak", "vietnam", "1.0.0"));
    }
}
