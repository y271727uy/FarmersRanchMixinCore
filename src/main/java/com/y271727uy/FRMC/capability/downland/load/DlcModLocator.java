package com.y271727uy.FRMC.capability.downland.load;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModLocator;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * FML locator that scans for DLC packs under {@code dlc/<id>/mods/}.
 * Self jar and root mods/ are handled by the built-in mods folder locator.
 */
public final class DlcModLocator extends AbstractJarFileModLocator {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public Stream<Path> scanCandidates() {
        Path gameDir = gameDir();
        if (gameDir == null) {
            return Stream.empty();
        }
        
        // Only scan DLC packs, not root mods/
        List<Path> dlcJars = DlcModScan.installedModJars(gameDir);
        LOGGER.info("FRMC DLC locator found {} DLC jar(s) from installed packs", dlcJars.size());
        
        return dlcJars.stream();
    }

    @Override
    public String name() {
        return "FRMC DLC mods";
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
    }

    private static Path gameDir() {
        try {
            Path path = FMLPaths.GAMEDIR.get();
            return path == null ? null : path.toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            LOGGER.warn("FRMC DLC locator has no game directory", exception);
            return null;
        }
    }
}
