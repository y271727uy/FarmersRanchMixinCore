package com.y271727uy.FRMC.mixin.kubejs;

import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.capability.downland.load.DlcKubejsScan;
import dev.latvian.mods.kubejs.script.ScriptFileInfo;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptPack;
import dev.latvian.mods.kubejs.script.ScriptPackInfo;
import dev.latvian.mods.kubejs.script.ScriptSource;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.Map;

/**
 * After KubeJS loads {@code kubejs/<type>_scripts/}, append installed DLC packs
 * from {@code dlc/<id>/kubejs/<type>_scripts/}. Root scripts stay authoritative.
 */
@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.script.ScriptManager", remap = false)
public abstract class ScriptManagerLoadMixin {
    @Unique
    private static final Logger frmc$LOGGER = LogUtils.getLogger();

    @Shadow
    public ScriptType scriptType;

    @Shadow
    public Map<String, ScriptPack> packs;

    @Invoker("loadFile")
    protected abstract void frmc$loadFile(ScriptPack pack, ScriptFileInfo info, ScriptSource source);

    @Inject(method = "loadFromDirectory", at = @At("RETURN"), require = 0)
    private void frmc$loadDlcScripts(CallbackInfo ci) {
        Path gameDir = frmc$gameDir();
        if (gameDir == null || scriptType == null) {
            return;
        }
        try {
            for (DlcKubejsScan.DlcKubejsPack dlc : DlcKubejsScan.installedScripts(gameDir, scriptType.name)) {
                frmc$loadPack(dlc);
            }
        } catch (RuntimeException exception) {
            frmc$LOGGER.warn("FRMC DLC kubejs scan failed", exception);
        }
    }

    @Unique
    private void frmc$loadPack(DlcKubejsScan.DlcKubejsPack dlc) {
        try {
            ScriptPack pack = new ScriptPack((ScriptManager) (Object) this, new ScriptPackInfo(dlc.namespace(), ""));
            ScriptSource.FromPath source = info -> dlc.directory().resolve(info.file);
            for (String file : dlc.files()) {
                try {
                    frmc$loadFile(pack, new ScriptFileInfo(pack.info, file), source);
                } catch (RuntimeException exception) {
                    frmc$LOGGER.warn("FRMC DLC kubejs skipped {} from {}", file, dlc.packId(), exception);
                }
            }
            pack.scripts.sort(null);
            packs.put(dlc.namespace(), pack);
        } catch (RuntimeException exception) {
            frmc$LOGGER.warn("FRMC DLC kubejs failed to load pack {}", dlc.packId(), exception);
        }
    }

    @Unique
    private static Path frmc$gameDir() {
        try {
            Path path = FMLPaths.GAMEDIR.get();
            return path == null ? null : path.toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            frmc$LOGGER.warn("FRMC DLC kubejs has no game directory", exception);
            return null;
        }
    }
}
