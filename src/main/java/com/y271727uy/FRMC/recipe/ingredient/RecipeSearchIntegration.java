package com.y271727uy.FRMC.recipe.ingredient;

import com.y271727uy.FRMC.mixin.minecraft.recipe.accessor.IngredientAccessor;
import com.y271727uy.FRMC.mixin.minecraft.recipe.accessor.ItemValueAccessor;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.ObjIntConsumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.PartialNBTIngredient;
import net.minecraftforge.common.crafting.StrictNBTIngredient;

public final class RecipeSearchIntegration {
    private static final Map<Class<?>, BiConsumer<?, ObjIntConsumer<Item>>> CUSTOM =
            new Reference2ReferenceOpenHashMap<>();
    static {
        registerSingleItemIngredient(PartialNBTIngredient.class);
        registerSingleItemIngredient(StrictNBTIngredient.class);
    }

    private RecipeSearchIntegration() {
    }

    @SuppressWarnings("unchecked")
    public static BiConsumer<Ingredient, ObjIntConsumer<Item>> getCustomIngredientAction(Class<?> type) {
        return (BiConsumer<Ingredient, ObjIntConsumer<Item>>) (BiConsumer<?, ?>) CUSTOM.get(type);
    }

    public static synchronized <T extends Ingredient> void registerCustomIngredientAction(
            Class<T> type,
            BiConsumer<T, ObjIntConsumer<Item>> action
    ) {
        CUSTOM.put(type, action);
    }

    private static <T extends Ingredient> void registerSingleItemIngredient(Class<T> type) {
        registerCustomIngredientAction(type, (ingredient, consumer) -> {
            Ingredient.Value[] values = ((IngredientAccessor) ingredient).frmc$getValues();
            if (values.length != 1 || !(values[0] instanceof Ingredient.ItemValue itemValue)) {
                return;
            }
            Item item = ((ItemValueAccessor) itemValue).frmc$getItem().getItem();
            if (item != Items.AIR) {
                consumer.accept(item, BuiltInRegistries.ITEM.getKey(item).hashCode());
            }
        });
    }
}
