package com.y271727uy.FRMC.mixin.manors_bounty;

import net.mcreator.manors_bounty.init.ManorsBountyModMobEffects;
import net.mcreator.manors_bounty.network.ManorsBountyModVariables.MapVariables;
import net.mcreator.manors_bounty.procedures.DoFruitEffectEnableCheckProcedure;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;
import java.util.function.Supplier;

@Mixin(targets = "net.mcreator.manors_bounty.procedures.FruitEffectUsingProcedure")
public abstract class FruitEffectUsingProcedureMixin {
    @Unique
    private static final TagKey<Item> FRMC$HAS_FRUIT_EFFECT_FOODS = frmc$itemTag("has_fruit_effect_foods");
    @Unique
    private static final TagKey<Item> FRMC$A_TYPE_FOODS = frmc$itemTag("a_type_foods");
    @Unique
    private static final TagKey<Item> FRMC$RARE_TYPE_FOODS = frmc$itemTag("rare_type_foods");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_ROSA_HEDGE = frmc$itemTag("effect_rosa_hedge");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_RUTIN_LEMONENE = frmc$itemTag("effect_rutin_lemonene");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_BURSTING_BERRY = frmc$itemTag("effect_bursting_berry");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_CHERRY_BLOSSOMS_WEEPING = frmc$itemTag("effect_cherry_blossoms_weeping");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_MOMENTARY_METEOR = frmc$itemTag("effect_momentary_meteor");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_SUMMER_HEATWAVE = frmc$itemTag("effect_summer_heatwave");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_ORIGINAL_EVOLUTION = frmc$itemTag("effect_original_evolution");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_MELON_GRAVITY = frmc$itemTag("effect_melon_gravity");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_SEA_TOUCH = frmc$itemTag("effect_sea_touch");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_HACKED_THORNS = frmc$itemTag("effect_hacked_thorns");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_KIWING_WHEREABOUTS = frmc$itemTag("effect_kiwing_whereabouts");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_LURKING_DANGER = frmc$itemTag("effect_lurking_danger");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_GEM_EXCAVATION = frmc$itemTag("effect_gem_excavation");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_POISON_HEAL_MEDICINE = frmc$itemTag("effect_poison_heal_medicine");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_HAPPY_AURA = frmc$itemTag("effect_happy_aura");
    @Unique
    private static final TagKey<Item> FRMC$EFFECT_DARK_WRATH = frmc$itemTag("effect_dark_wrath");

    /**
     * @author FRMC
     * @reason N
     */
    @Overwrite(remap = false)
    private static void execute(Event event, LevelAccessor world, Entity entity, ItemStack itemstack) {
        if (!(entity instanceof LivingEntity livingEntity) || livingEntity.level().isClientSide()) {
            return;
        }
        if (!DoFruitEffectEnableCheckProcedure.execute(world)) {
            return;
        }
        if (!frmc$isSupportedFruit(itemstack) || itemstack.is(FRMC$A_TYPE_FOODS)) {
            return;
        }

        UseAnim useAnim = itemstack.getUseAnimation();
        boolean rareFood = itemstack.is(FRMC$RARE_TYPE_FOODS);
        MapVariables variables = MapVariables.get(world);

        int bTypeLevel = 3;
        int bTypeTime = (int) variables.b_type_foods_effect_time;
        int cTypeLevel = 7;
        int cTypeTime = (int) variables.c_type_foods_effect_time;
        int rareBTypeLevel = 5;
        int rareBTypeTime = (int) variables.rare_b_type_foods_effect_time;
        int rareCTypeLevel = 9;
        int rareCTypeTime = (int) variables.rare_c_type_foods_effect_time;

        frmc$applyStandardEffect(livingEntity, itemstack, useAnim, rareFood, FRMC$EFFECT_ROSA_HEDGE, () -> ManorsBountyModMobEffects.ROSA_HEDGE.get(), bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeTime, rareBTypeLevel, rareCTypeTime, rareCTypeLevel);
        frmc$applyStandardEffect(livingEntity, itemstack, useAnim, rareFood, FRMC$EFFECT_RUTIN_LEMONENE, () -> ManorsBountyModMobEffects.RUTIN_LEMONENE.get(), bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeTime, rareBTypeLevel, rareCTypeTime, rareCTypeLevel);
        frmc$applyStandardEffect(livingEntity, itemstack, useAnim, rareFood, FRMC$EFFECT_BURSTING_BERRY, () -> ManorsBountyModMobEffects.BURSTING_BERRY.get(), bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeTime, rareBTypeLevel, rareCTypeTime, rareCTypeLevel);
        frmc$applyStandardEffect(livingEntity, itemstack, useAnim, rareFood, FRMC$EFFECT_CHERRY_BLOSSOMS_WEEPING, () -> ManorsBountyModMobEffects.CHERRY_BLOSSOMS_WEEPING.get(), bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeTime, rareBTypeLevel, rareCTypeTime, rareCTypeLevel);
        frmc$applyStandardEffect(livingEntity, itemstack, useAnim, rareFood, FRMC$EFFECT_MOMENTARY_METEOR, () -> ManorsBountyModMobEffects.MOMENTARY_METEOR.get(), bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeTime, rareBTypeLevel, rareCTypeTime, rareCTypeLevel);
        frmc$applyStandardEffect(livingEntity, itemstack, useAnim, rareFood, FRMC$EFFECT_SUMMER_HEATWAVE, () -> ManorsBountyModMobEffects.SUMMER_HEATWAVE.get(), bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeTime, rareBTypeLevel, rareCTypeTime, rareCTypeLevel);
        frmc$applyStandardEffect(livingEntity, itemstack, useAnim, rareFood, FRMC$EFFECT_ORIGINAL_EVOLUTION, () -> ManorsBountyModMobEffects.ORIGINAL_EVOLUTION.get(), bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeTime, rareBTypeLevel, rareCTypeTime, rareCTypeLevel);
        frmc$applyStandardEffect(livingEntity, itemstack, useAnim, rareFood, FRMC$EFFECT_MELON_GRAVITY, () -> ManorsBountyModMobEffects.MELON_GRAVITY.get(), bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeTime, rareBTypeLevel, rareCTypeTime, rareCTypeLevel);
        frmc$applyStandardEffect(livingEntity, itemstack, useAnim, rareFood, FRMC$EFFECT_SEA_TOUCH, () -> ManorsBountyModMobEffects.SEA_TOUCH.get(), bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeTime, rareBTypeLevel, rareCTypeTime, rareCTypeLevel);
        frmc$applyStandardEffect(livingEntity, itemstack, useAnim, rareFood, FRMC$EFFECT_GEM_EXCAVATION, () -> ManorsBountyModMobEffects.GEM_EXCAVATION.get(), bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeTime, rareBTypeLevel, rareCTypeTime, rareCTypeLevel);
        frmc$applyStandardEffect(livingEntity, itemstack, useAnim, rareFood, FRMC$EFFECT_POISON_HEAL_MEDICINE, () -> ManorsBountyModMobEffects.POISON_HEAL_MEDICINE.get(), bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeTime, rareBTypeLevel, rareCTypeTime, rareCTypeLevel);
        frmc$applyStandardEffect(livingEntity, itemstack, useAnim, rareFood, FRMC$EFFECT_HAPPY_AURA, () -> ManorsBountyModMobEffects.HAPPY_AURA.get(), bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeTime, rareBTypeLevel, rareCTypeTime, rareCTypeLevel);
        frmc$applyStandardEffect(livingEntity, itemstack, useAnim, rareFood, FRMC$EFFECT_DARK_WRATH, () -> ManorsBountyModMobEffects.DARK_WRATH.get(), bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeTime, rareBTypeLevel, rareCTypeTime, rareCTypeLevel);

        frmc$applyHackedThorns(livingEntity, itemstack, useAnim, rareFood, bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeLevel, rareCTypeTime, rareCTypeLevel);
        frmc$applyKiwingWhereabouts(livingEntity, itemstack, useAnim, rareFood, bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeTime, rareBTypeLevel, rareCTypeTime, rareCTypeLevel);
        frmc$applyLurkingDanger(livingEntity, itemstack, useAnim, rareFood, bTypeTime, bTypeLevel, cTypeTime, cTypeLevel, rareBTypeTime, rareBTypeLevel);
    }

    @Unique
    private static boolean frmc$isSupportedFruit(ItemStack itemstack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(itemstack.getItem());
        return itemstack.is(FRMC$HAS_FRUIT_EFFECT_FOODS) || itemId != null && "manors_bounty".equals(itemId.getNamespace());
    }

    @Unique
    private static void frmc$applyStandardEffect(
            LivingEntity entity,
            ItemStack itemstack,
            UseAnim useAnim,
            boolean rareFood,
            TagKey<Item> effectTag,
            Supplier<MobEffect> effectSupplier,
            int bTypeTime,
            int bTypeLevel,
            int cTypeTime,
            int cTypeLevel,
            int rareBTypeTime,
            int rareBTypeLevel,
            int rareCTypeTime,
            int rareCTypeLevel
    ) {
        if (!itemstack.is(effectTag)) {
            return;
        }

        if (rareFood) {
            if (useAnim == UseAnim.DRINK) {
                frmc$addEffect(entity, effectSupplier, rareBTypeTime, rareBTypeLevel, true);
            } else if (useAnim == UseAnim.EAT) {
                frmc$addEffect(entity, effectSupplier, rareCTypeTime, rareCTypeLevel, true);
            }
            return;
        }

        if (useAnim == UseAnim.DRINK) {
            frmc$addEffect(entity, effectSupplier, bTypeTime, bTypeLevel, false);
        } else if (useAnim == UseAnim.EAT) {
            frmc$addEffect(entity, effectSupplier, cTypeTime, cTypeLevel, false);
        }
    }

    @Unique
    private static void frmc$applyHackedThorns(
            LivingEntity entity,
            ItemStack itemstack,
            UseAnim useAnim,
            boolean rareFood,
            int bTypeTime,
            int bTypeLevel,
            int cTypeTime,
            int cTypeLevel,
            int rareBTypeLevel,
            int rareCTypeTime,
            int rareCTypeLevel
    ) {
        if (!itemstack.is(FRMC$EFFECT_HACKED_THORNS)) {
            return;
        }

        if (rareFood) {
            if (useAnim == UseAnim.DRINK) {
                frmc$addEffect(entity, () -> ManorsBountyModMobEffects.HACKED_THORNS.get(), bTypeLevel, rareBTypeLevel, true);
            } else if (useAnim == UseAnim.EAT) {
                frmc$addEffect(entity, () -> ManorsBountyModMobEffects.HACKED_THORNS.get(), rareCTypeTime, rareCTypeLevel, true);
            }
            return;
        }

        if (useAnim == UseAnim.DRINK) {
            frmc$addEffect(entity, () -> ManorsBountyModMobEffects.HACKED_THORNS.get(), bTypeTime, bTypeLevel, false);
        } else if (useAnim == UseAnim.EAT) {
            frmc$addEffect(entity, () -> ManorsBountyModMobEffects.HACKED_THORNS.get(), cTypeTime, cTypeLevel, false);
        }
    }

    @Unique
    private static void frmc$applyKiwingWhereabouts(
            LivingEntity entity,
            ItemStack itemstack,
            UseAnim useAnim,
            boolean rareFood,
            int bTypeTime,
            int bTypeLevel,
            int cTypeTime,
            int cTypeLevel,
            int rareBTypeTime,
            int rareBTypeLevel,
            int rareCTypeTime,
            int rareCTypeLevel
    ) {
        if (!itemstack.is(FRMC$EFFECT_KIWING_WHEREABOUTS)) {
            return;
        }

        if (rareFood) {
            if (useAnim == UseAnim.DRINK) {
                frmc$addEffect(entity, () -> ManorsBountyModMobEffects.KIWING_WHEREABOUTS.get(), rareBTypeTime, rareBTypeLevel, true);
            } else {
                frmc$addEffect(entity, () -> ManorsBountyModMobEffects.KIWING_WHEREABOUTS.get(), rareCTypeTime, rareCTypeLevel, true);
            }
            return;
        }

        if (useAnim == UseAnim.DRINK) {
            frmc$addEffect(entity, () -> ManorsBountyModMobEffects.KIWING_WHEREABOUTS.get(), bTypeTime, bTypeLevel, false);
        } else if (useAnim == UseAnim.EAT) {
            frmc$addEffect(entity, () -> ManorsBountyModMobEffects.KIWING_WHEREABOUTS.get(), cTypeTime, cTypeLevel, false);
        }
    }

    @Unique
    private static void frmc$applyLurkingDanger(
            LivingEntity entity,
            ItemStack itemstack,
            UseAnim useAnim,
            boolean rareFood,
            int bTypeTime,
            int bTypeLevel,
            int cTypeTime,
            int cTypeLevel,
            int rareBTypeTime,
            int rareBTypeLevel
    ) {
        if (!itemstack.is(FRMC$EFFECT_LURKING_DANGER)) {
            return;
        }

        if (rareFood) {
            frmc$addEffect(entity, () -> ManorsBountyModMobEffects.LURKING_DANGER.get(), rareBTypeTime, rareBTypeLevel, true);
            return;
        }

        if (useAnim == UseAnim.DRINK) {
            frmc$addEffect(entity, () -> ManorsBountyModMobEffects.LURKING_DANGER.get(), bTypeTime, bTypeLevel, false);
        } else if (useAnim == UseAnim.EAT) {
            frmc$addEffect(entity, () -> ManorsBountyModMobEffects.LURKING_DANGER.get(), cTypeTime, cTypeLevel, false);
        }
    }

    @Unique
    private static void frmc$addEffect(LivingEntity entity, Supplier<MobEffect> effectSupplier, int duration, int amplifier, boolean rareParticles) {
        entity.addEffect(new MobEffectInstance(effectSupplier.get(), duration, amplifier, false, rareParticles));
    }

    @Unique
    private static TagKey<Item> frmc$itemTag(String path) {
        return ItemTags.create(Objects.requireNonNull(ResourceLocation.tryParse("manors_bounty:" + path)));
    }
}

