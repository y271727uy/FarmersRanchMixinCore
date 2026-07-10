package com.y271727uy.FRMC.mixin.minecraft.recipe.manager;

import com.mojang.authlib.GameProfile;
import com.y271727uy.FRMC.recipe.manager.RecipeManagement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla RecipeManagement on the client side
 * with the optimized RecipeManagement that uses decision tree acceleration.
 */
@Pseudo
@Mixin(value = ClientPacketListener.class, priority = 2000)
public class ClientPacketListenerMixin {

    @Shadow
    @Final
    @Mutable
    private net.minecraft.world.item.crafting.RecipeManager recipeManager;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void frmc$createManager(Minecraft minecraft, Screen screen, Connection connection,
                                    ServerData serverData, GameProfile gameProfile,
                                    WorldSessionTelemetryManager worldSessionTelemetryManager,
                                    CallbackInfo ci) {
        recipeManager = new RecipeManagement(net.minecraftforge.common.crafting.conditions.ICondition.IContext.EMPTY);
    }
}
