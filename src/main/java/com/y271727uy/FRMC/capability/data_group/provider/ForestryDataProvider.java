package com.y271727uy.FRMC.capability.data_group.provider;

import com.y271727uy.FRMC.capability.data_group.target.BlockTarget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Data provider for Forestry trees.
 * Extracts tree species from items and blocks using Forestry API.
 */
public final class ForestryDataProvider implements DataProvider {
    private static final String PROVIDER_ID = "forestry";
    private static final ResourceLocation SAPLING_ITEM = new ResourceLocation("forestry", "sapling");
    private static final ResourceLocation SAPLING_BLOCK = new ResourceLocation("forestry", "sapling_ge");
    private static final String LEAVES_SUFFIX = "_default_leaves";
    private static final String FRUIT_LEAVES_SUFFIX = "_default_leaves_fruit";

    private final boolean forestryLoaded;

    public ForestryDataProvider() {
        this.forestryLoaded = ModList.get().isLoaded("forestry");
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    @Nullable
    public String getItemField(ItemStack stack, String fieldName) {
        if (!forestryLoaded || stack.isEmpty()) {
            return null;
        }

        if ("tree_species".equals(fieldName)) {
            return getTreeSpeciesFromItem(stack);
        }

        return null;
    }

    @Override
    @Nullable
    public String getBlockField(BlockTarget target, String fieldName) {
        if (!forestryLoaded) {
            return null;
        }

        if ("tree_species".equals(fieldName)) {
            return getTreeSpeciesFromBlock(target);
        }

        return null;
    }

    @Override
    public boolean supportsItem(ItemStack stack) {
        if (!forestryLoaded || stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return SAPLING_ITEM.equals(itemId);
    }

    @Override
    public boolean supportsBlock(BlockTarget target) {
        if (!forestryLoaded) {
            return false;
        }
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(target.getState().getBlock());
        if (SAPLING_BLOCK.equals(blockId)) {
            return true;
        }
        return blockId != null
            && "forestry".equals(blockId.getNamespace())
            && (blockId.getPath().endsWith(LEAVES_SUFFIX)
                || blockId.getPath().endsWith(FRUIT_LEAVES_SUFFIX));
    }

    @Nullable
    private String getTreeSpeciesFromItem(ItemStack stack) {
        try {
            // Use Forestry API to get tree species
            Class<?> handlerClass = Class.forName("forestry.api.genetics.capability.IIndividualHandlerItem");
            Class<?> treeTypeClass = Class.forName("forestry.core.utils.SpeciesUtil");
            
            // Get TREE_TYPE lazy instance
            var treeTypeLazy = treeTypeClass.getDeclaredField("TREE_TYPE").get(null);
            var speciesType = ((net.minecraftforge.common.util.Lazy<?>) treeTypeLazy).get();
            
            // Get species from item
            var getSpeciesMethod = handlerClass.getDeclaredMethod("getSpecies", 
                ItemStack.class, 
                Class.forName("forestry.api.genetics.ISpeciesType"));
            
            Object species = getSpeciesMethod.invoke(null, stack, speciesType);
            if (species == null) {
                return null;
            }
            
            // Get species ID
            var idMethod = species.getClass().getMethod("id");
            ResourceLocation speciesId = (ResourceLocation) idMethod.invoke(species);
            
            return speciesId != null ? speciesId.toString() : null;
            
        } catch (Exception e) {
            // Forestry API not available or changed
            return null;
        }
    }

    @Nullable
    private String getTreeSpeciesFromBlock(BlockTarget target) {
        try {
            BlockEntity blockEntity = target.getBlockEntity();
            if (blockEntity == null) {
                return null;
            }
            
            String blockEntityClass = blockEntity.getClass().getName();
            if (!blockEntityClass.equals("forestry.arboriculture.tiles.TileSapling")
                && !blockEntityClass.equals("forestry.arboriculture.tiles.TileLeaves")) {
                return null;
            }
            
            // Call getTree()
            var getTreeMethod = blockEntity.getClass().getMethod("getTree");
            Object tree = getTreeMethod.invoke(blockEntity);
            if (tree == null) {
                return null;
            }
            
            // Get species from tree
            var getSpeciesMethod = tree.getClass().getMethod("getSpecies");
            Object species = getSpeciesMethod.invoke(tree);
            if (species == null) {
                return null;
            }
            
            // Get species ID
            var idMethod = species.getClass().getMethod("id");
            ResourceLocation speciesId = (ResourceLocation) idMethod.invoke(species);
            
            return speciesId != null ? speciesId.toString() : null;
            
        } catch (Exception e) {
            // Forestry API not available or changed
            return null;
        }
    }
}
