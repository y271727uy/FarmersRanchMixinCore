package com.y271727uy.FRMC.mixin.untamed_wilds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import untamedwilds.entity.ComplexMob;
import untamedwilds.util.EntityDataHolder;
import untamedwilds.util.EntityDataHolderClient;
import untamedwilds.util.EntityUtils;

@Pseudo
@Mixin(value = EntityUtils.class, remap = false)
public abstract class EntityUtilsMixin {
    private static final ResourceLocation FRMC$MISSING_TEXTURE = new ResourceLocation("missing");

    /**
     * Resolves incomplete texture-cache entries to Minecraft's missing texture rather than
     * dereferencing an absent species or variant list on the render thread.
     *
     * @author FRMC
     * @reason Untamed Wilds dereferences texture cache layers before validating they exist.
     */
    @Overwrite
    public static ResourceLocation getSkinFromEntity(ComplexMob complexMob) {
        Optional<ResourceKey<EntityType<?>>> entityKey = ForgeRegistries.ENTITY_TYPES.getResourceKey(complexMob.getType());
        if (entityKey.isEmpty()) {
            return FRMC$MISSING_TEXTURE;
        }

        String entityId = entityKey.get().location().getPath();
        int variant = complexMob.getVariant();
        int skin = complexMob.getSkin();

        if (skin > 99) {
            ResourceLocation rareTexture = frmc$getTexture(ComplexMob.TEXTURES_RARE, entityId, variant, skin - 100);
            if (rareTexture != null) {
                return rareTexture;
            }
        }

        ResourceLocation commonTexture = frmc$getTexture(ComplexMob.TEXTURES_COMMON, entityId, variant, skin);
        return commonTexture != null ? commonTexture : FRMC$MISSING_TEXTURE;
    }

    /**
     * @author FRMC
     * @reason The original fallback of 99 creates fictitious creative-tab variants before data loading completes.
     */
    @Overwrite
    public static int getNumberOfSpecies(EntityType<?> entityType) {
        EntityDataHolder serverData = ComplexMob.ENTITY_DATA_HASH.get(entityType);
        if (serverData != null) {
            return serverData.getSpeciesData().size();
        }

        EntityDataHolderClient clientData = ComplexMob.CLIENT_DATA_HASH.get(entityType);
        if (clientData != null) {
            return clientData.getNumberOfSpecies();
        }

        return 1;
    }

    private static ResourceLocation frmc$getTexture(
        HashMap<String, HashMap<Integer, ArrayList<ResourceLocation>>> textures,
        String entityId,
        int variant,
        int skin
    ) {
        if (textures == null || variant < 0) {
            return null;
        }

        HashMap<Integer, ArrayList<ResourceLocation>> variants = textures.get(entityId);
        if (variants == null) {
            return null;
        }

        ArrayList<ResourceLocation> skins = variants.get(variant);
        if (skins == null || skins.isEmpty()) {
            return null;
        }

        int boundedSkin = Math.max(0, Math.min(skin, skins.size() - 1));
        return skins.get(boundedSkin);
    }
}
