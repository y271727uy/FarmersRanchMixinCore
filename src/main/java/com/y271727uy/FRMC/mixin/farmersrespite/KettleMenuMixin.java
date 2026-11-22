package com.y271727uy.FRMC.mixin.farmersrespite;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.farmersrespite.KettleSlots;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import umpaz.farmersrespite.common.block.entity.KettleBlockEntity;
import umpaz.farmersrespite.common.block.entity.container.KettleMenu;

@Mixin(value = KettleMenu.class, remap = false)
public abstract class KettleMenuMixin {
    @Unique
    private static final String FRMC_QUICK_MOVE =
            "(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;";

    @Shadow @Final public ItemStackHandler inventory;

    @ModifyConstant(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;"
            + "Lumpaz/farmersrespite/common/block/entity/KettleBlockEntity;"
            + "Lnet/minecraft/world/inventory/ContainerData;)V", constant = @Constant(intValue = 2), require = 1)
    private int frmc$addAllInputs(int original) {
        return KettleSlots.INPUT_COUNT;
    }

    @WrapOperation(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;"
            + "Lumpaz/farmersrespite/common/block/entity/KettleBlockEntity;"
            + "Lnet/minecraft/world/inventory/ContainerData;)V", at = @At(value = "NEW", target =
            "(Lnet/minecraftforge/items/IItemHandler;III)Lnet/minecraftforge/items/SlotItemHandler;"), require = 1)
    private SlotItemHandler frmc$positionInputs(IItemHandler handler, int index, int x, int y,
                                                Operation<SlotItemHandler> original) {
        return original.call(handler, KettleSlots.inventoryInput(index),
                24 + 18 * KettleSlots.inputColumn(index), 17 + 18 * KettleSlots.inputRow(index));
    }

    @ModifyConstant(method = {"quickMoveStack" + FRMC_QUICK_MOVE, "m_7648_" + FRMC_QUICK_MOVE},
            constant = @Constant(intValue = 2), require = 1)
    private int frmc$quickMoveContainerIndex(int original) {
        return KettleSlots.MENU_CONTAINER;
    }

    @ModifyConstant(method = {"quickMoveStack" + FRMC_QUICK_MOVE, "m_7648_" + FRMC_QUICK_MOVE},
            constant = @Constant(intValue = 3), require = 1)
    private int frmc$quickMoveResultIndex(int original) {
        return KettleSlots.MENU_RESULT;
    }

    @ModifyArg(method = {"quickMoveStack" + FRMC_QUICK_MOVE, "m_7648_" + FRMC_QUICK_MOVE},
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraftforge/items/ItemStackHandler;getStackInSlot(I)Lnet/minecraft/world/item/ItemStack;"),
            index = 0, require = 1)
    private int frmc$keepDrinkDisplayIndex(int menuIndex) {
        return KettleBlockEntity.DRINK_DISPLAY_SLOT;
    }

    @ModifyConstant(method = {"clearCraftingContent", "m_6650_"}, constant = @Constant(intValue = 2), require = 1)
    private int frmc$clearAllInputs(int original) {
        return KettleSlots.INPUT_COUNT;
    }

    @ModifyArg(method = {"clearCraftingContent", "m_6650_"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraftforge/items/ItemStackHandler;setStackInSlot(ILnet/minecraft/world/item/ItemStack;)V"),
            index = 0, require = 1)
    private int frmc$clearInputInventoryIndex(int index) {
        return KettleSlots.inventoryInput(index);
    }

    @Inject(method = {"fillCraftSlotsStackedContents", "m_5816_"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$countBrewIngredients(StackedContents contents, CallbackInfo ci) {
        for (int index = 0; index < KettleSlots.INPUT_COUNT; index++) {
            contents.accountStack(inventory.getStackInSlot(KettleSlots.inventoryInput(index)));
        }
        ci.cancel();
    }

    @ModifyConstant(method = {"getGridWidth", "m_6635_"}, constant = @Constant(intValue = 1), require = 1)
    private int frmc$gridWidth(int original) {
        return 2;
    }

    @ModifyConstant(method = {"getResultSlotIndex", "m_6636_"}, constant = @Constant(intValue = 3), require = 1)
    private int frmc$resultSlot(int original) {
        return KettleSlots.MENU_RESULT;
    }

    @ModifyConstant(method = {"getSize", "m_6653_"}, constant = @Constant(intValue = 3), require = 1)
    private int frmc$kettleSlotCount(int original) {
        return KettleSlots.PLAYER_START;
    }

    @Inject(method = {"shouldMoveToInventory", "m_142157_"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$returnOnlyIngredients(int slot, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(slot >= 0 && slot < KettleSlots.INPUT_COUNT);
    }
}
