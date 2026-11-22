package com.y271727uy.FRMC.mixin.farmersrespite;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.integration.farmersrespite.KettleBrewing;
import com.y271727uy.FRMC.integration.farmersrespite.KettleFluidTank;
import com.y271727uy.FRMC.integration.farmersrespite.KettleFluidInput;
import com.y271727uy.FRMC.integration.farmersrespite.KettleSlots;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import umpaz.farmersrespite.common.block.entity.KettleBlockEntity;
import umpaz.farmersrespite.common.crafting.KettleRecipe;
import umpaz.farmersrespite.common.registry.FRRecipeTypes;
import vectorwing.farmersdelight.common.block.entity.SyncedBlockEntity;

@Mixin(value = KettleBlockEntity.class, remap = false)
public abstract class KettleBlockEntityMixin extends SyncedBlockEntity {
    @Shadow private boolean checkNewRecipe;
    @Shadow private int brewTime;
    @Shadow @Final private FluidTank fluidTank;

    @Unique private RecipeManager frmc$brewRecipeManager;
    @Unique private KettleRecipe frmc$activeBrew;

    protected KettleBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "createFluidTank", at = @At("RETURN"), cancellable = true, require = 1)
    private void frmc$allowMultipleFluids(CallbackInfoReturnable<FluidTank> cir) {
        cir.setReturnValue(new KettleFluidTank(cir.getReturnValue().getCapacity(), () -> {
            checkNewRecipe = true;
            setChanged();
            inventoryChanged();
        }));
    }

    @Inject(method = "getMatchingRecipe", at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$findBrewRecipe(RecipeWrapper items, CallbackInfoReturnable<Optional<KettleRecipe>> cir) {
        if (level == null || !(fluidTank instanceof KettleFluidTank tank)) {
            return;
        }
        RecipeManager recipes = level.getRecipeManager();
        if (checkNewRecipe || frmc$brewRecipeManager != recipes) {
            KettleRecipe best = null;
            int bestInputCount = 0;
            for (KettleRecipe candidate : recipes.getAllRecipesFor(FRRecipeTypes.BREWING.get())) {
                KettleBrewing.Batch batch = KettleBrewing.plan(candidate, tank);
                if (batch != null && batch.inputs().size() > bestInputCount && candidate.matches(items, level)
                        && tank.brew(batch, FluidAction.SIMULATE)) {
                    best = candidate;
                    bestInputCount = batch.inputs().size();
                }
            }
            if (frmc$activeBrew != null && frmc$activeBrew != best) {
                brewTime = 0;
            }
            frmc$activeBrew = best;
            frmc$brewRecipeManager = recipes;
            checkNewRecipe = false;
        }
        cir.setReturnValue(Optional.ofNullable(frmc$activeBrew));
    }

    @Inject(method = "canBrew", at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$canBrewAllFluids(KettleRecipe recipe, KettleBlockEntity kettle, CallbackInfoReturnable<Boolean> cir) {
        if (kettle.getFluidTank() instanceof KettleFluidTank tank) {
            KettleBrewing.Batch batch = KettleBrewing.plan(recipe, tank);
            cir.setReturnValue(batch != null && tank.brew(batch, FluidAction.SIMULATE));
        }
    }

    @WrapOperation(method = "processBrewing", at = @At(value = "INVOKE", target =
            "Lnet/minecraftforge/fluids/capability/templates/FluidTank;setFluid(Lnet/minecraftforge/fluids/FluidStack;)V"),
            require = 1)
    private void frmc$completeBrewBatch(FluidTank target, FluidStack legacyOutput, Operation<Void> original,
                                       @Local(argsOnly = true) KettleRecipe recipe,
                                       @Cancellable CallbackInfoReturnable<Boolean> cir) {
        if (!(target instanceof KettleFluidTank tank)) {
            original.call(target, legacyOutput);
            return;
        }
        KettleBrewing.Batch batch = KettleBrewing.plan(recipe, tank);
        if (batch == null || !tank.brew(batch, FluidAction.EXECUTE)) {
            cir.setReturnValue(false);
        }
    }

    @WrapMethod(method = "fluidExtract", require = 1)
    private ItemStack frmc$transferStoredFluids(KettleBlockEntity kettle, ItemStack input, ItemStack output,
                                               Operation<ItemStack> original) {
        if (!(kettle.getFluidTank() instanceof KettleFluidTank tank) || input.isEmpty()) {
            return original.call(kettle, input, output);
        }
        KettleFluidInput transfer = KettleFluidInput.from(kettle, input);
        if (transfer == null) {
            // Keep the original pouring loop from continuing into the next fluid with the same recipe.
            return tank.drainPrimaryOnly(() -> original.call(kettle, input, output));
        }
        ItemStack container = transfer.container();
        if (input.getCount() < transfer.itemCount() || !transfer.fits(tank)
                || (!container.isEmpty() && !output.isEmpty() && (!ItemStack.isSameItemSameTags(container, output)
                || container.getCount() + output.getCount() > output.getMaxStackSize()))) {
            return ItemStack.EMPTY;
        }
        tank.fill(transfer.fluid(), FluidAction.EXECUTE);
        input.shrink(transfer.itemCount());
        return container;
    }

    @ModifyConstant(method = {"createHandler", "getDroppableInventory"}, constant = @Constant(intValue = 5), require = 2)
    private int frmc$expandInventory(int original) {
        return KettleSlots.INVENTORY_SIZE;
    }

    @ModifyArg(method = {"load", "m_142466_"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraftforge/items/ItemStackHandler;deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V"), index = 0,
            require = 1)
    private CompoundTag frmc$expandSavedInventory(CompoundTag inventory) {
        if (inventory.contains("Size", Tag.TAG_INT) && inventory.getInt("Size") < KettleSlots.INVENTORY_SIZE) {
            CompoundTag expanded = inventory.copy();
            expanded.putInt("Size", KettleSlots.INVENTORY_SIZE);
            return expanded;
        }
        return inventory;
    }

    @ModifyConstant(method = {"hasInput", "processBrewing"}, constant = @Constant(intValue = 2), require = 2)
    private int frmc$visitAllInputs(int original) {
        return KettleSlots.INPUT_COUNT;
    }

    @ModifyArg(method = {"hasInput", "processBrewing"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraftforge/items/ItemStackHandler;getStackInSlot(I)Lnet/minecraft/world/item/ItemStack;"),
            index = 0, require = 3)
    private int frmc$inputInventoryIndex(int index) {
        return KettleSlots.inventoryInput(index);
    }
}
