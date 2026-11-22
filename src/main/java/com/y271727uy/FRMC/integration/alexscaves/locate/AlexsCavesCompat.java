/*
 * Ported from BiomeSpy 1.3.3 for Minecraft 1.20.1.
 * Copyright (c) 2025 MoePus
 *
 * This file is licensed under the GNU Lesser General Public License, version 3 or later.
 * See third_party/BiomeSpy-NOTICE.txt and third_party/BiomeSpy-LICENSE.txt.
 *
 * Modified for FRMC: package names, mixin wiring and the removals listed in the NOTICE.
 */
package com.y271727uy.FRMC.integration.alexscaves.locate;

import net.minecraftforge.fml.ModList;

public class AlexsCavesCompat {
    public static boolean ALEXS_CAVES_INSTALLED = false;

    static {
        ALEXS_CAVES_INSTALLED = ModList.get().isLoaded("alexscaves");
    }
}
