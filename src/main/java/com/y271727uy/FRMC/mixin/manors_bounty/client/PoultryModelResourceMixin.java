package com.y271727uy.FRMC.mixin.manors_bounty.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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

@Pseudo
@Mixin(value = {BresseChickenModel.class, MallardDuckModel.class, TurkeyModel.class}, remap = false)
public abstract class PoultryModelResourceMixin {
    @Unique
    private static final Map<String, ResourceLocation> FRMC$FIXED_RESOURCES = new ConcurrentHashMap<>();

    @WrapOperation(method = {"getModelResource", "getAnimationResource"}, at = @At(value = "NEW",
            target = "(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation frmc$reuseFixedResource(String namespace, String path, Operation<ResourceLocation> original) {
        if ("manors_bounty".equals(namespace)) {
            return FRMC$FIXED_RESOURCES.computeIfAbsent(path, key -> original.call(namespace, key));
        }
        return original.call(namespace, path);
    }
}
