package com.y271727uy.FRMC.mixin.sinytra_connector;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Watches Sinytra Connector's module-layer migration hack.
 * <p>
 * Connector force-moves specific JDK/platform modules (currently just
 * {@code authlib}) out of the boot layer so it can make them transformable.
 * This is a narrow, hardcoded operation today, but it runs very early
 * (before FRMC's own mod code initializes) and any future Connector update
 * that widens what gets migrated would otherwise go unnoticed. This mixin
 * only observes; it never blocks or alters the migration.
 */
@Pseudo
@Mixin(targets = "org.sinytra.connector.service.hacks.ModuleLayerMigrator", remap = false)
public abstract class ModuleLayerMigratorWatchMixin {
    private static final Logger FRMC$LOGGER = LogUtils.getLogger();

    @Inject(method = "moveModule", at = @At("RETURN"), remap = false, require = 0)
    private static void frmc$logModuleMigration(String moduleName, CallbackInfoReturnable<Object> cir) {
        FRMC$LOGGER.info(
            "[FRMC watch] Sinytra Connector migrated module '{}' out of the boot layer (result={})",
            moduleName,
            cir.getReturnValue()
        );
    }
}
