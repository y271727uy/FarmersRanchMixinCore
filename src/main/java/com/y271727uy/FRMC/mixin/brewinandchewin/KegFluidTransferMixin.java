package com.y271727uy.FRMC.mixin.brewinandchewin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.brewinandchewin.KegWorldPouring;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import umpaz.brewinandchewin.common.block.entity.KegBlockEntity;
import umpaz.brewinandchewin.common.crafting.KegFermentingRecipe;
import umpaz.brewinandchewin.common.crafting.KegPouringRecipe;
import umpaz.brewinandchewin.common.registry.BnCRecipeTypes;
import umpaz.brewinandchewin.common.utility.KegRecipeWrapper;

@Mixin(value = KegBlockEntity.class, remap = false)
public abstract class KegFluidTransferMixin extends BlockEntity implements KegWorldPouring {
    @Shadow @Final private ItemStackHandler inventory;
    @Shadow @Final private FluidTank fluidTank;
    @Shadow @Final private KegRecipeWrapper recipeWrapper;

    @Shadow
    public abstract Optional<KegPouringRecipe> getPouringRecipe(ItemStack stack);

    protected KegFluidTransferMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "fluidExtract", at = @At("HEAD"), cancellable = true)
    private void frmc$transferRecipeContainers(ItemStack input, int limit, boolean inGui, boolean creative,
                                                CallbackInfoReturnable<List<ItemStack>> cir) {
        cir.setReturnValue(frmc$interact(input, limit, inGui, creative, true));
    }

    @Override
    public List<ItemStack> frmc$pourIntoKeg(ItemStack input, int limit, boolean creative) {
        return frmc$interact(input, limit, false, creative, false);
    }

    @Override
    public boolean frmc$canTakeFromKeg(ItemStack container) {
        if (container.isEmpty() || level == null || fluidTank.isEmpty()) {
            return false;
        }
        KegPouringRecipe recipe = getPouringRecipe(container).orElse(null);
        if (recipe != null) {
            return recipe.getAmount() > 0 && fluidTank.getFluidAmount() >= recipe.getAmount()
                    && frmc$matches(container, recipe.getContainer(), recipe.isStrict());
        }
        var handler = FluidUtil.getFluidHandler(container.copyWithCount(1)).orElse(null);
        return handler != null && handler.getTanks() > 0 && handler.getFluidInTank(0).isEmpty()
                && frmc$transferCapability(container, false, false).isSuccess();
    }

    @Unique
    private List<ItemStack> frmc$interact(ItemStack input, int limit, boolean inGui, boolean creative,
                                         boolean canExtract) {
        if (input.isEmpty() || level == null) {
            return List.of();
        }
        KegPouringRecipe recipe = getPouringRecipe(input).orElse(null);
        if (recipe == null) {
            return frmc$transferFluidContainer(input, limit, inGui, creative, canExtract);
        }
        boolean bottling = frmc$matches(input, recipe.getContainer(), recipe.isStrict());
        if (bottling && (!canExtract || inGui && frmc$keepBaseInTank())) {
            return List.of();
        }
        return frmc$transfer(input, limit, inGui, creative, recipe);
    }

    @Unique
    private List<ItemStack> frmc$transferFluidContainer(ItemStack input, int limit, boolean inGui,
                                                        boolean creative, boolean canExtract) {
        if (limit <= 0) {
            return List.of();
        }
        ItemStack one = input.copyWithCount(1);
        var handler = FluidUtil.getFluidHandler(one).orElse(null);
        if (handler == null || handler.getTanks() == 0) {
            return List.of();
        }
        FluidStack contained = handler.getFluidInTank(0);
        boolean emptying = !contained.isEmpty();
        if (!emptying && (!canExtract || inGui && frmc$keepBaseInTank())) {
            return List.of();
        }
        if (emptying && fluidTank.isEmpty() && level.getRecipeManager()
                .getAllRecipesFor(BnCRecipeTypes.KEG_POURING.get()).stream()
                .noneMatch(recipe -> recipe.getFluid(one).isFluidEqual(contained))) {
            return List.of();
        }

        FluidActionResult preview = frmc$transferCapability(one, emptying, false);
        if (!preview.isSuccess() || inGui && !preview.getResult().isEmpty()
                && frmc$acceptedOutput(preview.getResult()) < preview.getResult().getCount()) {
            return List.of();
        }
        FluidActionResult actual = frmc$transferCapability(one, emptying, true);
        if (!actual.isSuccess()) {
            return List.of();
        }
        if (creative) {
            return List.of(input);
        }
        input.shrink(1);
        return List.of(actual.getResult());
    }

    @Unique
    private boolean frmc$keepBaseInTank() {
        FluidStack contents = fluidTank.getFluid();
        if (contents.isEmpty()) {
            return false;
        }
        var fluid = contents.getFluid();
        boolean isBase = false;
        boolean isProduct = false;
        for (KegFermentingRecipe recipe : level.getRecipeManager().getAllRecipesFor(BnCRecipeTypes.FERMENTING.get())) {
            if (recipe.getResultFluid() != null && fluid.isSame(recipe.getResultFluid())) {
                isProduct = true;
            }
            FluidStack base = recipe.getFluidIngredient();
            if (base != null && fluid.isSame(base.getFluid())) {
                isBase = true;
                if (recipe.matches(recipeWrapper, level)) {
                    return true;
                }
            }
        }
        // Base-only fluids can be loaded before the ingredient grid is complete.
        return isBase && !isProduct;
    }

    @Unique
    private FluidActionResult frmc$transferCapability(ItemStack one, boolean emptying, boolean execute) {
        return emptying ? FluidUtil.tryEmptyContainer(one, fluidTank, fluidTank.getCapacity(), null, execute)
                : FluidUtil.tryFillContainer(one, fluidTank, fluidTank.getCapacity(), null, execute);
    }

    @Unique
    private List<ItemStack> frmc$transfer(ItemStack input, int limit, boolean inGui, boolean creative,
                                          KegPouringRecipe recipe) {
        int amount = recipe.getAmount();
        if (amount <= 0 || limit <= 0) {
            return List.of();
        }

        ItemStack filled = recipe.assemble(recipeWrapper, level.registryAccess());
        boolean bottling = frmc$matches(input, recipe.getContainer(), recipe.isStrict());
        if (!bottling && (!recipe.canFill() || !frmc$matches(input, filled, recipe.isStrict()))) {
            return List.of();
        }
        ItemStack result = bottling ? filled : recipe.getContainer(input);
        FluidStack fluid = bottling ? fluidTank.getFluid() : recipe.getFluid(input);
        if (result.isEmpty() || fluid.isEmpty()) {
            return List.of();
        }

        int available = bottling
                ? fluidTank.drain(fluid.copy(), FluidAction.SIMULATE).getAmount()
                : fluidTank.fill(new FluidStack(fluid, fluidTank.getCapacity()), FluidAction.SIMULATE);
        int count = Math.min(Math.min(limit, input.getCount()), available / amount);
        if (inGui && count > 0) {
            count = frmc$acceptedOutput(result.copyWithCount(count));
        }
        if (count <= 0) {
            return List.of();
        }

        FluidStack transfer = new FluidStack(fluid, count * amount);
        if (bottling) {
            fluidTank.drain(transfer, FluidAction.EXECUTE);
        } else {
            fluidTank.fill(transfer, FluidAction.EXECUTE);
        }
        if (creative) {
            return List.of(input);
        }

        input.shrink(count);
        if (inGui) {
            return List.of(result.copyWithCount(count));
        }
        List<ItemStack> outputs = new ArrayList<>();
        while (count > 0) {
            int stackSize = Math.min(count, result.getMaxStackSize());
            outputs.add(result.copyWithCount(stackSize));
            count -= stackSize;
        }
        return outputs;
    }

    @Inject(method = "canFerment", at = @At("RETURN"), cancellable = true)
    private void frmc$validateBatch(KegFermentingRecipe recipe, KegBlockEntity keg,
                                   CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !frmc$batchFits(recipe)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getRecipeWithoutTemperature", at = @At("RETURN"), cancellable = true)
    private void frmc$validatePreview(CallbackInfoReturnable<Optional<KegFermentingRecipe>> cir) {
        cir.setReturnValue(cir.getReturnValue().filter(this::frmc$batchFits));
    }

    @Unique
    private boolean frmc$batchFits(KegFermentingRecipe recipe) {
        if (recipe.getResultFluid() != null) {
            FluidStack base = recipe.getFluidIngredient();
            // One tank cannot keep leftover base fluid alongside the finished drink.
            if (recipe.getResultItem() != null || recipe.getAmount() <= 0
                    || recipe.getAmount() > fluidTank.getCapacity()
                    || fluidTank.getFluidAmount() != (base == null ? 0 : base.getAmount())) {
                return false;
            }
        }
        if (recipe.getResultItem() != null) {
            ItemStack result = new ItemStack(recipe.getResultItem(), recipe.getAmount());
            return !result.isEmpty() && frmc$acceptedOutput(result) == result.getCount();
        }
        return recipe.getResultFluid() != null;
    }

    @WrapOperation(method = "processFermenting", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/fluids/capability/templates/FluidTank;setFluid(Lnet/minecraftforge/fluids/FluidStack;)V"))
    private void frmc$useRecipeYield(FluidTank tank, FluidStack result, Operation<Void> original,
                                     KegFermentingRecipe recipe, KegBlockEntity keg) {
        original.call(tank, new FluidStack(result.getRawFluid(), recipe.getAmount()));
    }

    @WrapOperation(method = "processFermenting", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/items/ItemStackHandler;getStackInSlot(I)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack frmc$keepServingContainer(ItemStackHandler handler, int slot,
                                                Operation<ItemStack> original) {
        return slot == KegBlockEntity.CONTAINER_SLOT ? ItemStack.EMPTY : original.call(handler, slot);
    }

    @Inject(method = "processFermenting", at = @At("RETURN"))
    private void frmc$serveFinishedDrink(KegFermentingRecipe recipe, KegBlockEntity keg,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && recipe.getResultFluid() != null) {
            ItemStack container = inventory.getStackInSlot(KegBlockEntity.CONTAINER_SLOT);
            List<ItemStack> served = frmc$transferReadyDrink(container);
            if (!served.isEmpty()) {
                inventory.insertItem(KegBlockEntity.OUTPUT_SLOT, served.get(0), false);
            }
        }
    }

    @Unique
    private List<ItemStack> frmc$transferReadyDrink(ItemStack container) {
        KegPouringRecipe pouring = getPouringRecipe(container).orElse(null);
        if (pouring == null || !frmc$matches(container, pouring.getContainer(), pouring.isStrict())) {
            return List.of();
        }
        return frmc$transfer(container, inventory.getSlotLimit(KegBlockEntity.OUTPUT_SLOT), true, false, pouring);
    }

    @Unique
    private int frmc$acceptedOutput(ItemStack result) {
        return result.getCount() - inventory.insertItem(KegBlockEntity.OUTPUT_SLOT, result, true).getCount();
    }

    @Unique
    private static boolean frmc$matches(ItemStack input, ItemStack expected, boolean strict) {
        return strict ? ItemStack.isSameItemSameTags(input, expected) : ItemStack.isSameItem(input, expected);
    }
}
