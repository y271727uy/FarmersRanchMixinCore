module frmc.mixincoremodapi {
    requires transitive frmc.mixinagent;
    requires static org.objectweb.asm.tree;
    requires static cpw.mods.modlauncher;

    exports com.y271727uy.frmc.mixincoremodapi;

    provides cpw.mods.modlauncher.api.ITransformationService
            with com.y271727uy.frmc.mixincoremodapi.MixinCoremodTransformationService;
}
