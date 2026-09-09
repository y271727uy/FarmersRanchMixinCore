package com.y271727uy.FRMC.integration.polymorph;

import com.illusivesoulworks.polymorph.common.crafting.RecipeSelection;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.loading.FMLLoader;

public final class PolymorphIntegration {
    private static final boolean LOADED =
            FMLLoader.getLoadingModList().getModFileById("polymorph") != null;

    private PolymorphIntegration() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    public static <T extends Recipe<C>, C extends Container> T getBlockEntityRecipe(
            RecipeType<T> type,
            C inventory,
            Level level,
            BlockEntity blockEntity
    ) {
        return RecipeSelection.getBlockEntityRecipe(type, inventory, level, blockEntity).orElse(null);
    }
}
