package com.y271727uy.FRMC.mixin.manors_bounty.client;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Stream;
import net.mcreator.manors_bounty.network.ManorsBountyModVariables;
import net.mcreator.manors_bounty.network.ManorsBountyModVariables.PlayerVariables;
import net.mcreator.manors_bounty.procedures.FruitEffectTipsTypeLinesProcedure;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "net.mcreator.manors_bounty.procedures.FruitEffectTipsProcedure", remap = false)
public abstract class FruitEffectTipsProcedureMixin {
    @Unique
    private static final TagKey<Item> FRMC$A_TYPE_FOODS = ItemTags.create(new ResourceLocation("manors_bounty", "a_type_foods"));
    @Unique
    private static final List<TagKey<Item>> FRMC$EFFECT_TAGS = Stream.of(
            "rosa_hedge", "rutin_lemonene", "bursting_berry", "cherry_blossoms_weeping",
            "momentary_meteor", "summer_heatwave", "original_evolution", "melon_gravity",
            "sea_touch", "hacked_thorns", "kiwing_whereabouts", "lurking_danger",
            "gem_excavation", "poison_heal_medicine", "happy_aura", "dark_wrath")
            .map(name -> ItemTags.create(new ResourceLocation("manors_bounty", "effect_" + name)))
            .toList();

    /**
     * @author FRMC
     * @reason Skip unrelated stacks before reading player data or formatting durations.
     */
    @Overwrite(remap = false)
    private static void execute(Event event, Entity entity, ItemStack stack, List<Component> tooltip) {
        if (entity == null || tooltip == null) {
            return;
        }

        boolean aType = stack.is(FRMC$A_TYPE_FOODS);
        int matches = 0;
        for (int i = 0; i < FRMC$EFFECT_TAGS.size(); i++) {
            if (stack.is(FRMC$EFFECT_TAGS.get(i))) {
                matches |= 1 << i;
            }
        }
        if (matches == 0 && !aType) {
            return;
        }

        PlayerVariables variables = entity.getCapability(ManorsBountyModVariables.PLAYER_VARIABLES)
                .orElseGet(PlayerVariables::new);
        if (!variables.do_fruit_effect_enable_client) {
            return;
        }

        String bTime = "";
        String cTime = "";
        String rareBTime = "";
        String rareCTime = "";
        if (matches != 0 && !aType) {
            DecimalFormat format = new DecimalFormat("##");
            bTime = " IV (" + format.format(variables.b_type_foods_effect_time_client / 1200.0D) + ":00)";
            cTime = " VIII (" + format.format(variables.c_type_foods_effect_time_client / 1200.0D) + ":00)";
            rareBTime = "VI (" + format.format(variables.rare_b_type_foods_effect_time_client / 1200.0D) + ":00)";
            rareCTime = "X (" + format.format(variables.rare_c_type_foods_effect_time_client / 1200.0D) + ":00)";
        }

        while (matches != 0) {
            int index = Integer.numberOfTrailingZeros(matches);
            ResourceLocation tag = FRMC$EFFECT_TAGS.get(index).location();
            String name = Component.translatable("effect.manors_bounty." + tag.getPath().substring("effect_".length())).getString();
            FruitEffectTipsTypeLinesProcedure.execute(stack, bTime, cTime, name, tag.toString(), rareBTime, rareCTime, tooltip);
            matches &= matches - 1;
        }
        if (aType && !Screen.hasShiftDown()) {
            tooltip.add(Component.literal(Component.translatable("tips.manors_bounty.fruit_effect_shift_check").getString()));
        }
    }
}
