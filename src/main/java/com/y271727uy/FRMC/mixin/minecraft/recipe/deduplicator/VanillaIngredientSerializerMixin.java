package com.y271727uy.FRMC.mixin.minecraft.recipe.deduplicator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.y271727uy.FRMC.util.IngredientHolderUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.VanillaIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Overwrites VanillaIngredientSerializer.parse to return cached Ingredient
 * instances from Item/TagKey, reducing memory usage and object creation.
 */
@Mixin(value = VanillaIngredientSerializer.class, remap = false)
public class VanillaIngredientSerializerMixin {

    /**
     * @author FRMC
     * @reason Deduplicate Ingredient instances by reusing cached ones from Item/TagKey
     */
    @Overwrite
    public Ingredient parse(JsonObject json) {
        var item = json.get("item");
        if (item != null) {
            Object o = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(item.getAsString()));
            if (o == null) throw new JsonSyntaxException("Unknown item '" + item + "'");
            return ((IngredientHolderUtil) o).fastrecipesearch$getIngredient();
        }
        var tag = json.get("tag");
        if (tag != null) {
            ResourceLocation resourcelocation = ResourceLocation.parse(tag.getAsString());
            Object tagkey = TagKey.create(Registries.ITEM, resourcelocation);
            return ((IngredientHolderUtil) tagkey).fastrecipesearch$getIngredient();
        } else {
            throw new JsonParseException("An ingredient entry needs either a tag or an item");
        }
    }
}
