package com.y271727uy.frmc.mixinagent;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchPluginIntrusionTest {
    @Test
    void insertsIntoModLauncherPluginMapWithoutReplacingAnExistingPlugin() {
        FakePlugins handler = new FakePlugins();
        assertTrue(Substratum.installInto(handler));
        Object first = handler.plugins.get("frmc_substratum");
        assertSame(Substratum.plugin(), first);
        assertTrue(Substratum.installInto(handler));
        assertSame(first, handler.plugins.get("frmc_substratum"));
    }

    static final class FakePlugins {
        private final Map<String, Object> plugins = new HashMap<>();
    }
}
