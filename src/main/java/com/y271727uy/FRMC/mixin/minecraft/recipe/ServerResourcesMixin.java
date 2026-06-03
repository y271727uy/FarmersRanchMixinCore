package com.y271727uy.FRMC.mixin.minecraft.recipe;

import com.y271727uy.FRMC.recipe.RecipeManagement;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla RecipeManagement in ReloadableServerResources
 * with the optimized RecipeManagement that uses decision tree acceleration.
 */
@Mixin(value = ReloadableServerResources.class, priority = 2000)
public class ServerResourcesMixin {

    @Final
    @Shadow
    @Mutable
    private net.minecraft.world.item.crafting.RecipeManager recipes;

    @Shadow(remap = false)
    @Final
    private ICondition.IContext context;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void frmc$onInit(RegistryAccess.Frozen registryAccess, FeatureFlagSet enabledFeatures,
                             Commands.CommandSelection commandSelection, int functionCompilationLevel,
                             CallbackInfo ci) {
        this.recipes = new RecipeManagement(context);
    }
}
