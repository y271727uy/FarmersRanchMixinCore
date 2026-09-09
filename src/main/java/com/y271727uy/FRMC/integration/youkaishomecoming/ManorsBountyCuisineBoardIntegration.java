package com.y271727uy.FRMC.integration.youkaishomecoming;

import dev.xkmc.youkaishomecoming.content.pot.table.item.IngredientTableItem;
import dev.xkmc.youkaishomecoming.content.pot.table.item.TableItemManager;
import dev.xkmc.youkaishomecoming.content.pot.table.item.VariantTableItemBase;
import dev.xkmc.youkaishomecoming.content.pot.table.model.VariantModelHolder;
import dev.xkmc.youkaishomecoming.content.pot.table.model.VariantModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public final class ManorsBountyCuisineBoardIntegration {
    private static final String MANORS_BOUNTY = "manors_bounty";
    private static final ResourceLocation SUMESHI_TEXTURE = new ResourceLocation(MANORS_BOUNTY, "item/sumeshi");
    private static final ResourceLocation KELP_TEXTURE = new ResourceLocation("youkaishomecoming", "block/table/kelp");
    private static final ResourceLocation SUMESHI_PLACED = new ResourceLocation("frmc", "sumeshi_placed");
    private static final ResourceLocation SUMESHI_BASE = new ResourceLocation("frmc", "sumeshi");
    private static final ResourceLocation SUMESHI_GUNKAN = new ResourceLocation("frmc", "sumeshi_gunkan");

    private static boolean registered;

    private ManorsBountyCuisineBoardIntegration() {
    }

    public static void registerCuisineBoardBase() {
        if (registered || !ModList.get().isLoaded(MANORS_BOUNTY)) {
            return;
        }
        registered = true;

        VariantModelHolder placed = new VariantModelHolder(TableItemManager.MANAGER, SUMESHI_PLACED);
        placed.put("rice", SUMESHI_TEXTURE);
        IngredientTableItem rice = TableItemManager.TABLE
                .with(placed, ManorsBountyCuisineBoardIntegration::sumeshiIngredient);
        bindSpreadParts(rice.asBase(SUMESHI_PLACED));

        VariantModelHolder shaped = new VariantModelHolder(TableItemManager.MANAGER, SUMESHI_BASE);
        shaped.put("rice", SUMESHI_TEXTURE);
        IngredientTableItem nigiri = rice.addNext(shaped);
        bindNigiriParts(nigiri.asBase(SUMESHI_BASE));

        VariantModelHolder gunkan = new VariantModelHolder(TableItemManager.MANAGER, SUMESHI_GUNKAN);
        gunkan.put("rice", SUMESHI_TEXTURE);
        gunkan.put("kelp", KELP_TEXTURE);
        bindGunkanParts(nigiri.with(gunkan, ManorsBountyCuisineBoardIntegration::noriIngredient).asBase(SUMESHI_GUNKAN));
        bindSushiExtras();
    }

    private static void bindSpreadParts(VariantTableItemBase base) {
        VariantModelPart ingredient = base.addPart("ingredient", 5);
        map(ingredient, "cucumber", "cucumber_slice", "culturaldelights:cut_cucumber");
        map(ingredient, "imitation_crab", "surimi_crab_stick", "youkaishomecoming:imitation_crab");
        map(ingredient, "avocado", "avocado_slice");
        map(ingredient, "ikura", "ikura", "youkaishomecoming:roe");
        map(ingredient, "nori", "nori");
    }

    private static void bindGunkanParts(VariantTableItemBase base) {
        VariantModelPart top = base.addPart("top", 2);
        map(top, "ikura", "ikura", "youkaishomecoming:roe");
        map(top, "shirako", "shirako");
        map(top, "mentaiko", "mentaiko");
        map(top, "caviar", "caviar");
        map(top, "cucumber", "cucumber_slice", "culturaldelights:cut_cucumber");

        VariantModelPart sauce = base.addPart("sauce", 1);
        map(sauce, "jalapeno", "jalapeno_powder", "spanishdelight:paprika");
        map(sauce, "salt", "salt_shaker");
    }

    private static void bindNigiriParts(VariantTableItemBase base) {
        VariantModelPart gunkan = base.addPart("gunkan", 1);
        map(gunkan, "caviar", "caviar");
        map(gunkan, "mentaiko", "mentaiko");

        VariantModelPart top = base.addPart("top", 1);
        map(top, "salmon", "raw_salmon_slice");
        map(top, "cod", "raw_cod_slice");
        map(top, "fugu", "fugu_slice", "crabbersdelight:pufferfish_slice");
        map(top, "tuna", "tropical_fish_slice", "crabbersdelight:tropical_fish_slice");
        map(top, "otoro", "cooked_abalone");
        map(top, "flesh", Items.BEEF);
        map(top, "spam", "cooked_spam_slice");
        map(top, "prawn", "cooked_prawn", "braziliandelight:cooked_shrimp");
        map(top, "tempura", "fried_tempura");
        map(top, "cucumber", "cucumber_slice", "culturaldelights:cut_cucumber");
        map(top, "imitation_crab", "surimi_crab_stick", "youkaishomecoming:imitation_crab");
        map(top, "avocado", "avocado_slice");

        VariantModelPart nugget = base.addPart("nugget", 1);
        map(nugget, "gold", Items.GOLD_NUGGET);

        VariantModelPart sauce = base.addPart("sauce", 1);
        map(sauce, "jalapeno", "jalapeno_powder", "spanishdelight:paprika");
        map(sauce, "salt", "salt_shaker");

        VariantModelPart kelp = base.addPart("kelp", 1);
        map(kelp, "kelp", "nori");
    }

    private static void bindSushiExtras() {
        TableItemManager.SUSHI_KELP.addMapping("nori", ManorsBountyCuisineBoardIntegration::noriIngredient);
        TableItemManager.SUSHI_SAUCE.addMapping("salt", () -> manorsItem("salt_shaker"));
        TableItemManager.SUSHI_SAUCE.addMapping("jalapeno", () -> items("jalapeno_powder", "spanishdelight:paprika"));
        TableItemManager.SUSHI_TOP.addMapping("spam", () -> manorsItem("cooked_spam_slice"));
    }

    private static void map(VariantModelPart part, String modelId, String... itemIds) {
        part.addMapping(modelId, () -> items(itemIds));
    }

    private static void map(VariantModelPart part, String modelId, Item item) {
        part.addMapping(modelId, () -> Ingredient.of(item));
    }

    private static Ingredient sumeshiIngredient() {
        return manorsItem("sumeshi");
    }

    private static Ingredient noriIngredient() {
        return manorsItem("nori");
    }

    private static Ingredient manorsItem(String path) {
        return items(path);
    }

    private static Ingredient items(String... ids) {
        Item[] items = new Item[ids.length];
        int count = 0;
        for (String id : ids) {
            ResourceLocation location = id.indexOf(':') >= 0
                    ? new ResourceLocation(id)
                    : new ResourceLocation(MANORS_BOUNTY, id);
            Item item = ForgeRegistries.ITEMS.getValue(location);
            if (item != null) {
                items[count++] = item;
            }
        }
        if (count == 0) {
            return Ingredient.EMPTY;
        }
        if (count != items.length) {
            Item[] packed = new Item[count];
            System.arraycopy(items, 0, packed, 0, count);
            items = packed;
        }
        return Ingredient.of(items);
    }
}
