package com.y271727uy.FRMC.mixin.blacklist;

import com.y271727uy.FRMC.Config;
import com.y271727uy.FRMC.recipe.config.RecipeSearchConfig;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class FRMCMixinConfigPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        RecipeSearchConfig.load();
        FRMCMixinBlacklistConfig.load();
        FRMCMixinBlacklistExtension.register();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (isRecipeMixin(mixinClassName)) {
            if (!Config.recipeSearchEnabled) {
                return false;
            }
            if (mixinClassName.contains(".recipe.sync.")) {
                return Config.recipeIngredientSyncEnabled;
            }
            if (mixinClassName.contains(".recipe.deduplicator.")) {
                return Config.recipeIngredientDeduplicatorEnabled;
            }
        }
        return true;
    }

    private static boolean isRecipeMixin(String mixinClassName) {
        return mixinClassName.startsWith("com.y271727uy.FRMC.mixin.minecraft.recipe.");
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}




