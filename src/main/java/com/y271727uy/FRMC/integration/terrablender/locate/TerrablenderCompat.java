/*
 * Ported from BiomeSpy 1.3.3 for Minecraft 1.20.1.
 * Copyright (c) 2025 MoePus
 *
 * This file is licensed under the GNU Lesser General Public License, version 3 or later.
 * See third_party/BiomeSpy-NOTICE.txt and third_party/BiomeSpy-LICENSE.txt.
 *
 * Modified for FRMC: package names, mixin wiring and the removals listed in the NOTICE.
 */
package com.y271727uy.FRMC.integration.terrablender.locate;

import net.minecraftforge.fml.ModList;

public class TerrablenderCompat {
    public static boolean TERRABLENDER_INSTALLED = false;

    static {
        TERRABLENDER_INSTALLED = ModList.get().isLoaded("terrablender");
    }
}
