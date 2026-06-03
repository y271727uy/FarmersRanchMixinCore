package com.y271727uy.FRMC.mixin.blacklist;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FRMCMixinBlacklistConfigTest {
    @Test
    void parsesExactAndWildcardEntries() {
        String toml = """
            blacklisted_mixins = [
              "example.mod.mixin.ExactMixin",
              "example.mod.mixin.problematic.*"
            ]
            """;

        FRMCMixinBlacklistConfig.ParsedBlacklist parsed = FRMCMixinBlacklistConfig.parse(toml);

        assertTrue(parsed.matches("example.mod.mixin.ExactMixin"));
        assertTrue(parsed.matches("example.mod.mixin.problematic.SomeMixin"));
        assertFalse(parsed.matches("example.mod.mixin.SafeMixin"));
    }

    @Test
    void ignoresCommentsAndMissingSection() {
        String toml = """
            # no blacklist configured yet
            something_else = ["value"]
            """;

        FRMCMixinBlacklistConfig.ParsedBlacklist parsed = FRMCMixinBlacklistConfig.parse(toml);

        assertFalse(parsed.matches("example.mod.mixin.AnyMixin"));
    }
}


