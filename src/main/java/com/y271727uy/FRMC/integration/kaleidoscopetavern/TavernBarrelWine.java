package com.y271727uy.FRMC.integration.kaleidoscopetavern;

import com.github.ysbbbbbb.kaleidoscopetavern.datamap.data.DrinkEffectData;
import com.github.ysbbbbbb.kaleidoscopetavern.datamap.resources.DrinkEffectDataReloadListener;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopetavern.item.BottleBlockItem;
import com.github.ysbbbbbb.kaleidoscopetavern.item.DrinkBlockItem;
import com.y271727uy.FRMC.FRMCMod;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

public final class TavernBarrelWine {
    public static final TagKey<Item> OUTPUT = ItemTags.create(new ResourceLocation(FRMCMod.MODID, "tavern_barrel_wine"));
    public static final ResourceLocation COCKTAIL_DISPLAY = new ResourceLocation("kaleidoscope_tavern", "filled_cocktail_glass");
    private static final ResourceLocation HAH_MEAD = new ResourceLocation("hearthandharvest", "mead");
    private static final ResourceLocation HAH_SWEET_BERRY_WINE = new ResourceLocation("hearthandharvest", "sweet_berry_wine");

    private TavernBarrelWine() {
    }

    @SubscribeEvent
    public static void remapOldMugItem(MissingMappingsEvent event) {
        Item tankard = ForgeRegistries.ITEMS.getValue(TavernMugs.TANKARD_ID);
        if (tankard == null || tankard == Items.AIR) {
            return;
        }
        for (var mapping : event.getMappings(Registries.ITEM, TavernMugs.EMPTY_ID.getNamespace())) {
            if (mapping.getKey().equals(TavernMugs.EMPTY_ID)) {
                mapping.remap(tankard);
            }
        }
    }

    public static boolean isCocktailDisplay(BlockState state) {
        return COCKTAIL_DISPLAY.equals(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }

    public static boolean isYoukaiBottleRecipe(ResourceLocation id) {
        return id.getNamespace().equals(FRMCMod.MODID) && id.getPath().startsWith("tavern_barrel/youkaishomecoming/");
    }

    public static boolean usesConfiguredYield(ResourceLocation id) {
        return isYoukaiBottleRecipe(id)
                || isAdaptedBarrelRecipe(id) && id.getPath().startsWith("tavern_barrel/yeastnfeast/");
    }

    public static boolean isAdaptedBarrelRecipe(ResourceLocation id) {
        return id != null && id.getNamespace().equals(FRMCMod.MODID) && id.getPath().startsWith("tavern_barrel/");
    }

    public static boolean isExternalOutput(ItemStack stack) {
        return stack.is(OUTPUT) && !(stack.getItem() instanceof DrinkBlockItem);
    }

    public static boolean hasExternalQuality(ItemStack stack) {
        return !(stack.getItem() instanceof BottleBlockItem) && stack.getTag() != null
                && stack.getTag().contains("BrewLevel", Tag.TAG_ANY_NUMERIC);
    }

    public static Component qualityLine(int quality) {
        Component name = Component.translatable("message.kaleidoscope_tavern.barrel.brew_level." + quality);
        return Component.translatable("tooltip.kaleidoscope_tavern.bottle_block.brew_level", name)
            .withStyle(ChatFormatting.GRAY);
    }

    @SubscribeEvent
    public static void onDrinkFinished(LivingEntityUseItemEvent.Finish event) {
        LivingEntity drinker = event.getEntity();
        ItemStack stack = event.getItem();
        if (drinker.level().isClientSide || !isExternalOutput(stack) || stack.getUseAnimation() != UseAnim.DRINK) {
            return;
        }
        for (DrinkEffectData.Entry entry : effectsAtQuality(stack, BottleBlockItem.getBrewLevel(stack))) {
            if (drinker.level().getRandom().nextFloat() >= entry.probability()) {
                continue;
            }
            if (entry.effect().isInstantenous()) {
                entry.effect().applyInstantenousEffect(drinker, drinker, drinker, entry.amplifier(), 1.0D);
            } else {
                drinker.addEffect(asEffectInstance(entry));
            }
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        boolean hasWineProfile = isExternalOutput(stack);
        if (!hasWineProfile && !hasExternalQuality(stack)) {
            return;
        }
        int quality = BottleBlockItem.getBrewLevel(stack);
        if (quality == 0) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        tooltip.add(qualityLine(quality));
        if (!hasWineProfile || stack.getUseAnimation() != UseAnim.DRINK) {
            return;
        }
        List<MobEffectInstance> guaranteed = effectsAtQuality(stack, quality).stream()
            .filter(entry -> entry.probability() >= 1.0F)
            .map(TavernBarrelWine::asEffectInstance)
            .toList();
        if (!guaranteed.isEmpty()) {
            tooltip.add(Component.empty());
            PotionUtils.addPotionTooltip(guaranteed, tooltip, 1.0F);
        }
    }

    private static List<DrinkEffectData.Entry> effectsAtQuality(ItemStack stack, int quality) {
        if (quality == 0) {
            return List.of();
        }
        var profiles = DrinkEffectDataReloadListener.INSTANCE;
        DrinkEffectData profile = profiles.get(stack.getItem());
        if (profile == null) {
            profile = profiles.get(defaultProfileItem(stack.getItem()));
        }
        if (profile == null || profile.effects().isEmpty()) {
            return List.of();
        }
        // Tavern uses the final defined tier for qualities above the data map's length.
        return profile.effects().get(Math.min(quality, profile.effects().size()) - 1);
    }

    private static Item defaultProfileItem(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (HAH_MEAD.equals(id)) {
            return ModItems.HONEY_WINE.get();
        }
        if (HAH_SWEET_BERRY_WINE.equals(id)) {
            return ModItems.SWEET_BERRY_WINE.get();
        }
        return ModItems.WINE.get();
    }

    private static MobEffectInstance asEffectInstance(DrinkEffectData.Entry entry) {
        return new MobEffectInstance(entry.effect(), entry.duration() * 20, entry.amplifier());
    }
}
