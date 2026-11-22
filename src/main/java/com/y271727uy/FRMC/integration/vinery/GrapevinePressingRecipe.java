package com.y271727uy.FRMC.integration.vinery;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.ForgeRegistries;

public record GrapevinePressingRecipe(ResourceLocation id, Ingredient ingredient, int count, Ingredient additive,
                                     Item container, ItemStack result, boolean white) implements Recipe<Container> {
    public static final int MAX_INPUT = 8;

    public int totalInputs() {
        return count + (additive == Ingredient.EMPTY ? 0 : 1);
    }

    @Override
    public boolean matches(Container inventory, Level level) {
        return ingredient.test(inventory.getItem(0));
    }

    @Override
    public ItemStack assemble(Container inventory, RegistryAccess registries) {
        return result.copy();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return result.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return additive == Ingredient.EMPTY ? NonNullList.of(Ingredient.EMPTY, ingredient)
            : NonNullList.of(Ingredient.EMPTY, ingredient, additive);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GrapevinePressingRegistry.SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return GrapevinePressingRegistry.TYPE;
    }

    public static final class Serializer implements RecipeSerializer<GrapevinePressingRecipe> {
        @Override
        public GrapevinePressingRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getNonNull(json, "ingredient"));
            int count = GsonHelper.getAsInt(json, "count");
            Ingredient additive = json.has("additive") ? Ingredient.fromJson(json.get("additive")) : Ingredient.EMPTY;
            if (count < 1 || count > MAX_INPUT - (additive == Ingredient.EMPTY ? 0 : 1)) {
                throw new JsonSyntaxException("Pressing requires between 1 and " + MAX_INPUT + " total inputs");
            }
            Item container = Items.AIR;
            if (json.has("container")) {
                ResourceLocation containerId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "container"));
                container = containerId == null ? null : ForgeRegistries.ITEMS.getValue(containerId);
                if (container == null || container == Items.AIR) {
                    throw new JsonSyntaxException("Pressing container must be a registered item");
                }
            }
            ItemStack result = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true);
            if (result.isEmpty() || result.getCount() > result.getMaxStackSize()) {
                throw new JsonSyntaxException("Pressing result must be a valid item stack");
            }
            return new GrapevinePressingRecipe(id, ingredient, count, additive, container, result,
                GsonHelper.getAsBoolean(json, "white", false));
        }

        @Override
        public GrapevinePressingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            int count = buffer.readVarInt();
            Ingredient additive = buffer.readBoolean() ? Ingredient.fromNetwork(buffer) : Ingredient.EMPTY;
            Item container = buffer.readItem().getItem();
            return new GrapevinePressingRecipe(id, ingredient, count, additive, container, buffer.readItem(), buffer.readBoolean());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, GrapevinePressingRecipe recipe) {
            recipe.ingredient.toNetwork(buffer);
            buffer.writeVarInt(recipe.count);
            buffer.writeBoolean(recipe.additive != Ingredient.EMPTY);
            if (recipe.additive != Ingredient.EMPTY) {
                recipe.additive.toNetwork(buffer);
            }
            buffer.writeItem(new ItemStack(recipe.container));
            buffer.writeItem(recipe.result);
            buffer.writeBoolean(recipe.white);
        }
    }
}
