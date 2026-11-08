package com.y271727uy.FRMC.mixin.manors_bounty.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.mcreator.manors_bounty.entity.model.BresseChickenModel;
import net.mcreator.manors_bounty.entity.model.MallardDuckModel;
import net.mcreator.manors_bounty.entity.model.TurkeyModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(value = {BresseChickenModel.class, MallardDuckModel.class, TurkeyModel.class}, remap = false)
public abstract class PoultryModelResourceMixin {
    @Unique
    private static final Map<String, ResourceLocation> FRMC$FIXED_RESOURCES = new ConcurrentHashMap<>();

    @Redirect(method = {"getModelResource", "getAnimationResource"}, at = @At(value = "NEW",
            target = "(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation frmc$reuseFixedResource(String namespace, String path) {
        if ("manors_bounty".equals(namespace)) {
            return FRMC$FIXED_RESOURCES.computeIfAbsent(path, key -> new ResourceLocation("manors_bounty", key));
        }
        return new ResourceLocation(namespace, path);
    }
}
