package com.y271727uy.FRMC.integration.vinery.jei;

import com.y271727uy.FRMC.FRMCMod;
import com.y271727uy.FRMC.integration.vinery.GrapevinePressingRegistry;
import com.y271727uy.FRMC.integration.vinery.TavernPressing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.satisfy.vinery.core.item.GrapeItem;
import net.satisfy.vinery.core.registry.GrapeTypeRegistry;
import net.satisfy.vinery.core.registry.ObjectRegistry;
import net.satisfy.vinery.core.util.GrapeType;

@JeiPlugin
public final class GrapevinePotJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = new ResourceLocation(FRMCMod.MODID, "grapevine_pot");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (ModList.get().isLoaded("vinery")) {
            registration.addRecipeCategories(new GrapevinePotRecipeCategory(
                registration.getJeiHelpers().getGuiHelper(), new ItemStack(ForgeRegistries.ITEMS.getValue(GrapevinePressingRegistry.POT))));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (ModList.get().isLoaded("vinery")) {
            registration.addRecipeCatalyst(new ItemStack(ForgeRegistries.ITEMS.getValue(GrapevinePressingRegistry.POT)),
                GrapevinePotRecipeCategory.TYPE);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!ModList.get().isLoaded("vinery")) {
            return;
        }
        List<GrapevinePotRecipeCategory.Recipe> recipes = new ArrayList<>();
        for (GrapeType type : GrapeTypeRegistry.GRAPE_TYPE_TYPES.stream().sorted().toList()) {
            if (type.getFruit() != Items.AIR && type.getBottle() != Items.AIR) {
                recipes.add(new GrapevinePotRecipeCategory.Recipe(List.of(new ItemStack(type.getFruit(), 6)), List.of(),
                    new ItemStack(ObjectRegistry.WINE_BOTTLE.get(), 2), new ItemStack(type.getBottle(), 2), false));
            }
        }
        if (ModList.get().isLoaded("kaleidoscope_tavern")) {
            for (TavernPressing pressing : TavernPressing.values()) {
                List<ItemStack> fruit = Arrays.stream(pressing.ingredient().getItems())
                    .filter(stack -> !stack.isEmpty() && TavernPressing.forInput(stack) == pressing)
                    .map(stack -> stack.copyWithCount(TavernPressing.BATCH_SIZE))
                    .toList();
                ItemStack output = pressing.output();
                if (!fruit.isEmpty() && !output.isEmpty()) {
                    boolean hasVineryGrape = fruit.stream().anyMatch(stack -> stack.getItem() instanceof GrapeItem);
                    recipes.add(new GrapevinePotRecipeCategory.Recipe(fruit, List.of(), new ItemStack(Items.BUCKET), output, hasVineryGrape));
                }
            }
        }
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            for (var pressing : connection.getRecipeManager().getAllRecipesFor(GrapevinePressingRegistry.TYPE)) {
                List<ItemStack> fruit = Arrays.stream(pressing.ingredient().getItems())
                    .filter(stack -> !stack.isEmpty())
                    .map(stack -> stack.copyWithCount(pressing.count()))
                    .toList();
                List<ItemStack> additive = Arrays.stream(pressing.additive().getItems()).filter(stack -> !stack.isEmpty()).toList();
                if (!fruit.isEmpty() && (pressing.totalInputs() == pressing.count() || !additive.isEmpty())) {
                    ItemStack container = new ItemStack(pressing.container());
                    recipes.add(new GrapevinePotRecipeCategory.Recipe(fruit, additive, container,
                        pressing.result().copy(), !container.isEmpty()));
                }
            }
        }
        registration.addRecipes(GrapevinePotRecipeCategory.TYPE, recipes);
    }
}
