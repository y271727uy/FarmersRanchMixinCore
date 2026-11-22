package com.y271727uy.FRMC.mixin.kaleidoscopetavern.brewery.bac;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BarrelBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.BarrelBlockEntity;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import umpaz.brewinandchewin.common.item.BoozeItem;
import umpaz.brewinandchewin.common.registry.BnCItems;
import umpaz.brewinandchewin.common.registry.BnCRecipeTypes;

@Mixin(value = BarrelBlock.class, remap = false)
public abstract class BarrelTankardPourMixin {
    @Unique
    private static final ResourceLocation frmc$WHITE_RUM = new ResourceLocation("brewincompatdelight", "rum");
    @Unique
    private static final ResourceLocation frmc$BIRCH_SAP_BOTTLE = new ResourceLocation("cosmopolitan", "birch_sap_bottle");
    @Unique
    private static final ResourceLocation frmc$BIRCH_SAP = new ResourceLocation("cosmopolitan", "birch_sap");

    @Inject(method = {"use", "m_6227_"}, at = @At(value = "INVOKE",
        target = "Lcom/github/ysbbbbbb/kaleidoscopetavern/util/FluidUtils;isFluidContainer(Lnet/minecraft/world/item/ItemStack;)Z"),
        cancellable = true)
    private void frmc$pourDrinkBase(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir,
                                   @Local BarrelBlockEntity barrel) {
        ItemStack stack = player.getItemInHand(hand);
        var itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return;
        }
        FluidStack base;
        ItemStack emptyContainer;
        if (frmc$BIRCH_SAP_BOTTLE.equals(itemId) && ForgeRegistries.FLUIDS.containsKey(frmc$BIRCH_SAP)) {
            base = new FluidStack(ForgeRegistries.FLUIDS.getValue(frmc$BIRCH_SAP), 250);
            emptyContainer = new ItemStack(Items.GLASS_BOTTLE);
        } else if (stack.getItem() instanceof BoozeItem drink
                && (stack.is(BnCItems.BEER.get()) || stack.is(BnCItems.STRONGROOT_ALE.get())
                || stack.is(BnCItems.MEAD.get()) || stack.is(BnCItems.BLOODY_MARY.get())
                || frmc$WHITE_RUM.equals(itemId))) {
            base = new FluidStack(drink.getFluid(), 250);
            emptyContainer = new ItemStack(BnCItems.TANKARD.get());
        } else if (frmc$isManorsBaseBottle(itemId)) {
            var pouring = level.getRecipeManager().getAllRecipesFor(BnCRecipeTypes.KEG_POURING.get()).stream()
                    .filter(recipe -> recipe.canFill() && recipe.getOutput().is(stack.getItem())
                            && (!recipe.isStrict() || ItemStack.isSameItemSameTags(recipe.getOutput(), stack)))
                    .findFirst().orElse(null);
            if (pouring == null || pouring.getAmount() <= 0) {
                return;
            }
            base = pouring.getFluid(stack);
            emptyContainer = pouring.getContainer(stack).copy();
        } else {
            return;
        }

        if (base.isEmpty() || emptyContainer.isEmpty()) {
            return;
        }

        // This injection runs only on the center of an open lid.
        var ingredients = barrel.getIngredient();
        for (int slot = 0; slot < ingredients.getSlots(); slot++) {
            if (!ingredients.getStackInSlot(slot).isEmpty()) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }
        }
        if (barrel.isBrewing() || barrel.getFluid().fill(base, FluidAction.SIMULATE) != base.getAmount()) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        if (!level.isClientSide) {
            barrel.getFluid().fill(base, FluidAction.EXECUTE);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                ItemHandlerHelper.giveItemToPlayer(player, emptyContainer);
            }
            barrel.refresh();
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
    }

    @Unique
    private static boolean frmc$isManorsBaseBottle(ResourceLocation id) {
        return switch (id.toString()) {
            case "manors_bounty:sake", "manors_bounty:bottled_water" -> true;
            default -> false;
        };
    }
}
