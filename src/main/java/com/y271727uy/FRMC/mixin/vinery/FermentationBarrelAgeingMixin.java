package com.y271727uy.FRMC.mixin.vinery;

import com.y271727uy.FRMC.integration.vinery.VineryAgeing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.satisfy.vinery.core.util.WineYears;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.satisfy.vinery.core.block.entity.FermentationBarrelBlockEntity", remap = false)
public abstract class FermentationBarrelAgeingMixin extends BlockEntity {
    protected FermentationBarrelAgeingMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "isIngredient", at = @At("HEAD"), cancellable = true)
    private void frmc$acceptAgeingItemsFromHopper(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (VineryAgeing.accepts(stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = {"setItem", "m_6836_"}, at = @At("RETURN"), require = 1)
    private void frmc$stampYear(int slot, ItemStack stack, CallbackInfo ci) {
        Level world = getLevel();
        if (world == null || world.isClientSide || slot < 1 || slot > 3 || !VineryAgeing.accepts(stack)) {
            return;
        }
        if (!WineYears.hasWineYear(stack)) {
            stack.getOrCreateTag().putInt(WineYears.TAG_YEAR, WineYears.getYear(world));
            setChanged();
        }
    }
}
