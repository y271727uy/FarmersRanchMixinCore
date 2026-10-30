package com.y271727uy.FRMC.mixin.forestry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Makes Forestry fruit harvested by right-click drop into the world instead of entering the inventory. */
@Pseudo
@Mixin(targets = "forestry.arboriculture.blocks.BlockDefaultLeavesFruit", remap = false)
public abstract class BlockDefaultLeavesFruitHarvestMixin {
    @Redirect(
        method = "m_6227_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/items/ItemHandlerHelper;giveItemToPlayer(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
            remap = false
        ),
        require = 1,
        remap = false
    )
    private void frmc$dropHarvestedFruit(Player targetPlayer, ItemStack stack,
                                          BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return;
        }

        ItemEntity entity = new ItemEntity(
            level,
            pos.getX() + 0.5D,
            pos.getY() + 0.5D,
            pos.getZ() + 0.5D,
            stack.copy()
        );
        entity.setPickUpDelay(10);
        level.addFreshEntity(entity);
    }
}
