package com.y271727uy.FRMC.mixin.farmersdelight;

import com.y271727uy.FRMC.util.CookingPotLosslessContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity", remap = false)
public abstract class CookingPotBlockEntityMixin {
    @Shadow
    @Final
    private ItemStackHandler inventory;

    @Shadow
    public abstract boolean isContainerValid(ItemStack stack);

    @Inject(
        method = "useStoredContainersOnMeal",
        at = @At("HEAD"),
        cancellable = true,
        require = 1
    )
    private void frmc$useLosslessStoredContainer(CallbackInfo ci) {
        ItemStack meal = this.inventory.getStackInSlot(6);
        ItemStack container = this.inventory.getStackInSlot(7);
        ItemStack output = this.inventory.getStackInSlot(8);
        if (!CookingPotLosslessContainerHelper.isLosslessContainer(container) || !this.isContainerValid(container)) {
            return;
        }

        if (meal.isEmpty()) {
            return;
        }

        if (output.isEmpty()) {
            this.inventory.setStackInSlot(8, meal.split(1));
            ci.cancel();
            return;
        }

        if (output.getItem() != meal.getItem() || output.getCount() >= output.getMaxStackSize()) {
            return;
        }

        meal.shrink(1);
        output.grow(1);
        ci.cancel();
    }
}
