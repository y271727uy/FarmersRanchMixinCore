module frmc.mixinagent {
    requires transitive frmc.mixinasm;
    requires static transitive org.objectweb.asm.tree;
    requires static cpw.mods.modlauncher;
    requires java.instrument;

    exports com.y271727uy.frmc.mixinagent;
}
