package com.y271727uy.FRMC.integration.vinery;

import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class GrapevinePressingBatch extends BlockEntity {
    private Ingredient ingredient = Ingredient.EMPTY;
    private Ingredient additive = Ingredient.EMPTY;
    private ItemStack fruit = ItemStack.EMPTY;
    private ItemStack container = ItemStack.EMPTY;
    private ItemStack result = ItemStack.EMPTY;
    private int count;
    private boolean additiveAdded;

    public GrapevinePressingBatch(BlockPos pos, BlockState state) {
        super(GrapevinePressingRegistry.batchType, pos, state);
    }

    public void start(GrapevinePressingRecipe recipe, ItemStack input) {
        // A batch keeps the ingredients and yield it started with across reloads.
        ingredient = recipe.ingredient();
        additive = recipe.additive();
        additiveAdded = false;
        fruit = input.copyWithCount(1);
        container = new ItemStack(recipe.container());
        result = recipe.result().copy();
        count = recipe.count();
        setChanged();
    }

    public boolean acceptsPrimary(ItemStack stack, int stored) {
        return stored - (additiveAdded ? 1 : 0) < count && !stack.isEmpty()
            && (ingredient.test(stack) || (!fruit.isEmpty() && ItemStack.isSameItemSameTags(fruit, stack)));
    }

    public boolean acceptsAdditive(ItemStack stack) {
        return !additiveAdded && additive != Ingredient.EMPTY && !stack.isEmpty() && additive.test(stack);
    }

    public void addAdditive() {
        additiveAdded = true;
        setChanged();
    }

    public boolean usesContainer(ItemStack stack) {
        return container.isEmpty() ? stack.isEmpty() : stack.is(container.getItem());
    }

    public int totalInputs() {
        return count + (additive == Ingredient.EMPTY ? 0 : 1);
    }

    public ItemStack result() {
        return result.copy();
    }

    public ItemStack fruit() {
        return fruit.copy();
    }

    public ItemStack container() {
        return container.copy();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (ingredient != Ingredient.EMPTY) {
            tag.putString("Ingredient", ingredient.toJson().toString());
        }
        if (additive != Ingredient.EMPTY) {
            tag.putString("Additive", additive.toJson().toString());
            tag.putBoolean("AdditiveAdded", additiveAdded);
        }
        tag.put("Fruit", fruit.save(new CompoundTag()));
        tag.put("Container", container.save(new CompoundTag()));
        tag.put("Result", result.save(new CompoundTag()));
        tag.putInt("Count", count);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ingredient = tag.contains("Ingredient", Tag.TAG_STRING)
            ? Ingredient.fromJson(JsonParser.parseString(tag.getString("Ingredient"))) : Ingredient.EMPTY;
        additive = tag.contains("Additive", Tag.TAG_STRING)
            ? Ingredient.fromJson(JsonParser.parseString(tag.getString("Additive"))) : Ingredient.EMPTY;
        additiveAdded = tag.getBoolean("AdditiveAdded");
        fruit = ItemStack.of(tag.getCompound("Fruit"));
        container = ItemStack.of(tag.getCompound("Container"));
        result = ItemStack.of(tag.getCompound("Result"));
        count = tag.getInt("Count");
    }
}
