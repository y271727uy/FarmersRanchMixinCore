package com.y271727uy.FRMC.integration.ftbquests;

import com.y271727uy.FRMC.FRMCMod;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;

/**
 * Completes when the configured JEI category is shown and its category is an LDLib preview.
 * The task stores only a JEI recipe-type id, never a mod-specific structure.
 */
public final class PreviewTask extends Task {
    private static final int MAX_ID_LENGTH = 256;
    private static final String TYPE = "type";
    private static final String RECIPE = "recipe";
    private static TaskType taskType;

    private String recipeType = "";
    private String recipe = "";

    public PreviewTask(long id, Quest quest) {
        super(id, quest);
    }

    public static TaskType register() {
        taskType = TaskTypes.register(
                new ResourceLocation(FRMCMod.MODID, "preview"),
                PreviewTask::new,
                () -> Icon.getIcon("minecraft:item/spyglass"));
        taskType.setDisplayName(Component.translatable("ftbquests.task.frmc.preview"));
        return taskType;
    }

    @Override
    public TaskType getType() {
        return taskType;
    }

    @Override
    public void writeData(CompoundTag nbt) {
        super.writeData(nbt);
        if (!recipeType.isEmpty()) {
            nbt.putString(TYPE, recipeType);
        }
        if (!recipe.isEmpty()) {
            nbt.putString(RECIPE, recipe);
        }
    }

    @Override
    public void readData(CompoundTag nbt) {
        super.readData(nbt);
        recipeType = bounded(nbt.getString(TYPE));
        recipe = bounded(nbt.getString(RECIPE));
    }

    @Override
    public void writeNetData(FriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(recipeType, MAX_ID_LENGTH);
        buffer.writeUtf(recipe, MAX_ID_LENGTH);
    }

    @Override
    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);
        recipeType = bounded(buffer.readUtf(MAX_ID_LENGTH));
        recipe = bounded(buffer.readUtf(MAX_ID_LENGTH));
    }

    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addString("recipe_type", recipeType, value -> recipeType = bounded(value), "")
                .setNameKey("ftbquests.task.frmc.preview.recipe_type");
        config.addString("recipe", recipe, value -> recipe = bounded(value), "")
                .setNameKey("ftbquests.task.frmc.preview.recipe");
    }

    @Override
    public void onButtonClicked(Button button, boolean canClick) {
        if (!canClick || Minecraft.getInstance().player == null) {
            return;
        }
        button.playClickSound();
        if (!available()) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("frmc.ftbquests.preview.unavailable").withStyle(ChatFormatting.RED),
                    true);
            return;
        }
        long taskId = id;
        String type = recipeType;
        String recipeId = recipe;
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (PreviewRecipeOpener.open(type, recipeId)) {
                PreviewTaskNetwork.confirm(taskId, type, recipeId);
            }
        });
    }

    public boolean matches(String confirmedType, String confirmedRecipe) {
        if (!available() || !recipeType.equals(confirmedType)) {
            return false;
        }
        return recipe.isEmpty() || recipe.equals(confirmedRecipe);
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (!teamData.isCompleted(this) && checkTaskSequence(teamData) && available()) {
            teamData.setProgress(this, 1L);
        }
    }

    private boolean available() {
        return ModList.get().isLoaded("jei")
                && ModList.get().isLoaded("ldlib")
                && resource(recipeType) != null
                && (recipe.isEmpty() || resource(recipe) != null);
    }

    private static String bounded(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= MAX_ID_LENGTH ? trimmed : trimmed.substring(0, MAX_ID_LENGTH);
    }

    private static ResourceLocation resource(String value) {
        try {
            return value.isEmpty() ? null : new ResourceLocation(value);
        } catch (ResourceLocationException ignored) {
            return null;
        }
    }
}
