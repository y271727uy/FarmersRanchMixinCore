package com.y271727uy.FRMC.integration.ftbquests;

import com.y271727uy.FRMC.integration.ftbquests.client.GuideLinkClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;

/** Registers the generic preview task only when FTB Quests is present. */
public final class FtbQuestsIntegration {
    private static boolean registered;

    private FtbQuestsIntegration() {
    }

    public static void register() {
        if (registered || !ModList.get().isLoaded("ftbquests")) {
            return;
        }
        registered = true;
        PreviewTask.register();
        PreviewTaskNetwork.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> GuideLinkClient::registerParser);
    }
}
