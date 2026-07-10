package com.y271727uy.FRMC.mixin.mystiasizakaya;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.hiedacamellia.mystiasizakaya.core.entry.MIItem;
import org.hiedacamellia.mystiasizakaya.core.event.MIPlayerEvent;
import org.hiedacamellia.mystiasizakaya.registries.MITag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin {
    private static final BlockPos FRMC$EMPTY_MENU_POS = new BlockPos(-1, -1, -1);

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void frmc$handleMystiaLedgerMenu(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (!frmc$isMystiaLedger(heldStack)) {
            return;
        }

        ItemFrame itemFrame = (ItemFrame)(Object)this;
        ItemStack menuStack = itemFrame.getItem();
        if (!(menuStack.getItem() instanceof MIItem miItem)) {
            return;
        }

        if (player.level().isClientSide()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (player instanceof ServerPlayer serverPlayer && frmc$updateMystiaMenu(serverPlayer, itemFrame, menuStack, miItem)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    private static boolean frmc$isMystiaLedger(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && "mystias_izakaya".equals(key.getNamespace()) && "ledger".equals(key.getPath());
    }

    private static boolean frmc$updateMystiaMenu(ServerPlayer serverPlayer, ItemFrame itemFrame, ItemStack itemStack, MIItem miItem) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(miItem);
        if (key == null) {
            return false;
        }

        BlockPos blockPos = itemFrame.getPos();
        List<BlockPos> blockPosList = new ArrayList<>(MIPlayerEvent.getMenuBlockPos(serverPlayer));
        List<String> cuisineList = new ArrayList<>(MIPlayerEvent.getMenus(serverPlayer));
        List<String> beverageList = new ArrayList<>(MIPlayerEvent.getMenusBeverages(serverPlayer));
        frmc$padMenuLists(blockPosList, cuisineList, beverageList);

        for (int i = 0; i < blockPosList.size(); i++) {
            BlockPos pos = blockPosList.get(i);
            if (frmc$isSameMenuFrame(pos, blockPos) && serverPlayer.isShiftKeyDown()) {
                blockPosList.set(i, FRMC$EMPTY_MENU_POS);
                cuisineList.set(i, "minecraft:air");
                beverageList.set(i, "minecraft:air");
                serverPlayer.sendSystemMessage(Component.translatable("message.mystias_izakaya.menu.unbound", i + 1, blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                break;
            }

            if (frmc$isSameMenuFrame(pos, blockPos) && !serverPlayer.isShiftKeyDown()) {
                if (itemStack.is(MITag.cuisinesKey) && !cuisineList.contains(key.toString())) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.mystias_izakaya.menu.cuisine", itemStack.getDisplayName().getString(), i + 1));
                    cuisineList.set(i, key.toString());
                }

                if (itemStack.is(MITag.beveragesKey) && !beverageList.contains(key.toString())) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.mystias_izakaya.menu.beverage", itemStack.getDisplayName().getString(), i + 1));
                    beverageList.set(i, key.toString());
                }
                break;
            }

            if (Objects.equals(pos, FRMC$EMPTY_MENU_POS) && !serverPlayer.isShiftKeyDown()) {
                blockPosList.set(i, blockPos);
                serverPlayer.sendSystemMessage(Component.translatable("message.mystias_izakaya.menu.bound", i + 1, blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                break;
            }
        }

        MIPlayerEvent.setMenuBlockPos(serverPlayer, blockPosList);
        MIPlayerEvent.setMenus(serverPlayer, cuisineList);
        MIPlayerEvent.setMenusBeverages(serverPlayer, beverageList);
        return true;
    }

    private static boolean frmc$isSameMenuFrame(BlockPos existingPos, BlockPos clickedPos) {
        return existingPos.equals(clickedPos) || existingPos.above().equals(clickedPos) || existingPos.below().equals(clickedPos);
    }

    private static void frmc$padMenuLists(List<BlockPos> blockPosList, List<String> cuisineList, List<String> beverageList) {
        while (blockPosList.size() < 8) {
            blockPosList.add(FRMC$EMPTY_MENU_POS);
        }

        while (cuisineList.size() < blockPosList.size()) {
            cuisineList.add("minecraft:air");
        }

        while (beverageList.size() < blockPosList.size()) {
            beverageList.add("minecraft:air");
        }
    }
}
