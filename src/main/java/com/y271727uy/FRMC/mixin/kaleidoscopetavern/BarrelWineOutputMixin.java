package com.y271727uy.FRMC.mixin.kaleidoscopetavern;

import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.BarrelBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.DrinkBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopetavern.item.BottleBlockItem;
import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.TavernBarrelWine;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.TavernMugs;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BarrelBlockEntity.class, remap = false)
public abstract class BarrelWineOutputMixin {
    @Shadow
    private ResourceLocation recipeId;

    @Shadow
    public abstract int getBrewLevel();

    @Shadow
    public abstract void tipBrewInfo(LivingEntity player);

    @Shadow
    private void resetIfOutputEmpty() {
        throw new AssertionError();
    }

    @Inject(method = "canTapExtract", at = @At("HEAD"), cancellable = true)
    private void frmc$reportUnfinishedWine(Level level, BlockPos pos, LivingEntity player,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (frmc$waitingForWine()) {
            tipBrewInfo(player);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doTapExtract", at = @At("HEAD"), cancellable = true)
    private void frmc$waitForWine(Level level, BlockPos pos, CallbackInfo ci) {
        if (frmc$waitingForWine()) {
            ci.cancel();
        }
    }

    @Unique
    private boolean frmc$waitingForWine() {
        return getBrewLevel() < 2 && TavernBarrelWine.isAdaptedBarrelRecipe(recipeId);
    }

    @Inject(method = "extractOneOutput", at = @At("RETURN"))
    private void frmc$saveWineQuality(ItemStack recipeResult, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack output = cir.getReturnValue();
        if (!output.isEmpty() && !(output.getItem() instanceof BottleBlockItem)
                && (TavernBarrelWine.isAdaptedBarrelRecipe(recipeId) || TavernBarrelWine.isExternalOutput(output))) {
            BottleBlockItem.setBrewLevel(output, getBrewLevel());
        }
    }

    @Inject(method = "transformPlacedCarrier", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;", ordinal = 0),
            cancellable = true)
    private void frmc$fillPlacedBottle(Level level, BlockPos pos, BlockState carrier, ItemStack recipeResult,
                                       CallbackInfo ci, @Local(ordinal = 1) ItemStack output) {
        boolean woodenMug = TavernMugs.EMPTY_ID.equals(ForgeRegistries.BLOCKS.getKey(carrier.getBlock()));
        Block display = woodenMug ? ForgeRegistries.BLOCKS.getValue(TavernMugs.FILLED_ID)
            : frmc$glassDisplay(carrier.getBlock() == ModBlocks.EMPTY_GLASSWARE.get(), output);
        if (frmc$placeDrink(level, pos, carrier, output, display)) {
            resetIfOutputEmpty();
            ci.cancel();
        }
    }

    @Inject(method = "transformItemCarrier", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/item/ItemEntity;getItem()Lnet/minecraft/world/item/ItemStack;"),
            cancellable = true)
    private void frmc$fillDroppedCup(Level level, BlockPos pos, ItemEntity carrier, ItemStack recipeResult,
                                    CallbackInfo ci, @Local(ordinal = 1) ItemStack output) {
        BlockState previous = level.getBlockState(pos);
        if (!previous.isAir()) {
            return;
        }
        ItemStack emptyCups = carrier.getItem();
        ResourceLocation carrierId = ForgeRegistries.ITEMS.getKey(emptyCups.getItem());
        Block display = null;
        if (TavernMugs.TANKARD_ID.equals(carrierId)) {
            display = ForgeRegistries.BLOCKS.getValue(TavernMugs.FILLED_ID);
        } else if (emptyCups.is(ModBlocks.EMPTY_GLASSWARE.get().asItem())) {
            display = ForgeRegistries.BLOCKS.getValue(TavernBarrelWine.COCKTAIL_DISPLAY);
        }
        if (frmc$placeDrink(level, pos, previous, output, display)) {
            ItemStack remaining = emptyCups.copyWithCount(emptyCups.getCount() - 1);
            if (remaining.isEmpty()) {
                carrier.discard();
            } else {
                carrier.setItem(remaining);
            }
            resetIfOutputEmpty();
            ci.cancel();
        }
    }

    @Inject(method = "transformItemCarrier", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z", ordinal = 0),
            cancellable = true)
    private void frmc$fillDroppedBottle(Level level, BlockPos pos, ItemEntity carrier,
                                       ItemStack recipeResult, CallbackInfo ci, @Local(ordinal = 1) ItemStack output,
                                       @Local BlockState state) {
        if (state.isAir() && frmc$placeDrink(level, pos, state, output, frmc$glassDisplay(false, output))) {
            resetIfOutputEmpty();
            ci.cancel();
        }
    }

    @Unique
    private static Block frmc$glassDisplay(boolean cocktailGlass, ItemStack output) {
        if (cocktailGlass) {
            return ForgeRegistries.BLOCKS.getValue(TavernBarrelWine.COCKTAIL_DISPLAY);
        }
        return output.is(TavernBarrelWine.OUTPUT) || TavernBarrelWine.hasExternalQuality(output)
            ? ModBlocks.WINE.get() : null;
    }

    @Unique
    private static boolean frmc$placeDrink(Level level, BlockPos pos, BlockState previous, ItemStack output,
                                           Block display) {
        if (display == null) {
            return false;
        }

        BlockState filled = display.defaultBlockState();
        if (previous.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            filled = filled.setValue(BlockStateProperties.HORIZONTAL_FACING,
                    previous.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        if (display != ModBlocks.WINE.get() && previous.hasProperty(BlockStateProperties.WATERLOGGED)) {
            filled = filled.setValue(BlockStateProperties.WATERLOGGED, previous.getValue(BlockStateProperties.WATERLOGGED));
        }
        if (!level.setBlockAndUpdate(pos, filled)) {
            return false;
        }

        // DrinkBlock already returns the stored stack on pickup and through its loot handling.
        if (level.getBlockEntity(pos) instanceof DrinkBlockEntity bottle && bottle.addItem(output)) {
            bottle.refresh();
            return true;
        }
        level.setBlockAndUpdate(pos, previous);
        return false;
    }
}
