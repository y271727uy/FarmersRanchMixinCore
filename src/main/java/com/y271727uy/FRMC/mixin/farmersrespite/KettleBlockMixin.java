package com.y271727uy.FRMC.mixin.farmersrespite;

import com.y271727uy.FRMC.integration.farmersrespite.KettleFluidInput;
import com.y271727uy.FRMC.integration.farmersrespite.KettleFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import umpaz.farmersrespite.common.block.KettleBlock;
import umpaz.farmersrespite.common.block.entity.KettleBlockEntity;

@Mixin(value = KettleBlock.class, remap = false)
public abstract class KettleBlockMixin {
    @Inject(method = {"use", "m_6227_"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$pourFullContainer(BlockState state, Level level, BlockPos pos, Player player,
                                        InteractionHand hand, BlockHitResult hit,
                                        CallbackInfoReturnable<InteractionResult> cir) {
        if (!(level.getBlockEntity(pos) instanceof KettleBlockEntity kettle)
                || !(kettle.getFluidTank() instanceof KettleFluidTank tank)) {
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        KettleFluidInput transfer = KettleFluidInput.from(kettle, held);
        if (transfer == null || held.getCount() < transfer.itemCount()) {
            return;
        }
        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide) {
            return;
        }
        int amount = transfer.fluid().getAmount();
        if (amount > tank.getSpace()) {
            player.displayClientMessage(Component.translatable("frmc.kettle.not_enough_space", tank.getSpace(), amount), true);
            return;
        }
        if (!transfer.fits(tank)) {
            player.displayClientMessage(Component.translatable("frmc.kettle.too_many_fluids", tank.getTanks()), true);
            return;
        }
        tank.fill(transfer.fluid(), FluidAction.EXECUTE);
        if (!player.getAbilities().instabuild) {
            held.shrink(transfer.itemCount() - 1);
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, transfer.container()));
        }
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1, 1);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
    }
}
