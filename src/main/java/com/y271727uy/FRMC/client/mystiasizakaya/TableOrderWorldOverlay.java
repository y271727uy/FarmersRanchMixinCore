package com.y271727uy.FRMC.client.mystiasizakaya;

import com.mojang.blaze3d.vertex.PoseStack;
import com.y271727uy.FRMC.FRMCMod;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.hiedacamellia.mystiasizakaya.content.common.block.entities.TableEntity;
import org.hiedacamellia.mystiasizakaya.core.event.MIPlayerEvent;

@Mod.EventBusSubscriber(modid = FRMCMod.MODID, value = Dist.CLIENT)
public final class TableOrderWorldOverlay {
    private static final String MYSTIA_MOD_ID = "mystias_izakaya";
    private static final BlockPos UNBOUND_TABLE = new BlockPos(-1, -1, -1);
    private static final double MAX_RENDER_DISTANCE_SQUARED = 32.0D * 32.0D;
    private static final float ITEM_SCALE = 0.32F;
    private static final float TEXT_SCALE = 0.018F;

    private TableOrderWorldOverlay() {
    }

    @SubscribeEvent
    public static void renderTableOrders(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || !ModList.get().isLoaded(MYSTIA_MOD_ID)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }

        List<BlockPos> tables = MIPlayerEvent.getTables(minecraft.player);
        List<String> cuisineOrders = MIPlayerEvent.getOrders(minecraft.player);
        List<String> beverageOrders = MIPlayerEvent.getOrdersBeverages(minecraft.player);
        int size = Math.min(tables.size(), Math.min(cuisineOrders.size(), beverageOrders.size()));
        if (size == 0) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        for (int i = 0; i < size; i++) {
            BlockPos tablePos = tables.get(i);
            if (UNBOUND_TABLE.equals(tablePos)) {
                continue;
            }

            ItemStack cuisine = getItemStack(cuisineOrders.get(i));
            ItemStack beverage = getItemStack(beverageOrders.get(i));
            if (cuisine.isEmpty() && beverage.isEmpty()) {
                continue;
            }

            Vec3 center = Vec3.atCenterOf(tablePos).add(0.0D, 1.05D, 0.0D);
            if (center.distanceToSqr(cameraPos) > MAX_RENDER_DISTANCE_SQUARED) {
                continue;
            }

            boolean cuisineReady = false;
            boolean beverageReady = false;
            BlockEntity blockEntity = minecraft.level.getBlockEntity(tablePos);
            if (blockEntity instanceof TableEntity tableEntity) {
                List<ItemStack> tableItems = tableEntity.getItems();
                cuisineReady = containsSameItem(tableItems, cuisine);
                beverageReady = containsSameItem(tableItems, beverage);
            }

            poseStack.pushPose();
            poseStack.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z);
            poseStack.mulPose(event.getCamera().rotation());
            renderOrderCard(poseStack, bufferSource, minecraft, i + 1, cuisine, beverage, cuisineReady, beverageReady);
            poseStack.popPose();
        }

        bufferSource.endBatch();
    }

    private static void renderOrderCard(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        Minecraft minecraft,
        int tableNumber,
        ItemStack cuisine,
        ItemStack beverage,
        boolean cuisineReady,
        boolean beverageReady
    ) {
        renderItem(poseStack, bufferSource, minecraft, cuisine, -0.24F, 0.03F, tableNumber * 2);
        renderItem(poseStack, bufferSource, minecraft, beverage, 0.24F, 0.03F, tableNumber * 2 + 1);

        String title = "\u9910\u684c #" + tableNumber;
        drawCenteredText(poseStack, bufferSource, minecraft.font, title, -32.0F, 0xFFFFFFFF);
        if (cuisineReady) {
            drawText(poseStack, bufferSource, minecraft.font, "\u2713", 18.0F, -8.0F, 0xFF28FF28);
        }

        if (beverageReady) {
            drawText(poseStack, bufferSource, minecraft.font, "\u2713", -18.0F, -8.0F, 0xFF28FF28);
        }
    }

    private static void renderItem(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        Minecraft minecraft,
        ItemStack stack,
        float x,
        float y,
        int seed
    ) {
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(x, y, -0.03F);
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        minecraft.getItemRenderer()
            .renderStatic(
                stack,
                ItemDisplayContext.GUI,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                minecraft.level,
                seed
            );
        poseStack.popPose();
    }

    private static void drawCenteredText(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        Font font,
        String text,
        float y,
        int color
    ) {
        drawText(poseStack, bufferSource, font, text, -font.width(text) / 2.0F, y, color);
    }

    private static void drawText(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        Font font,
        String text,
        float x,
        float y,
        int color
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, -0.06F);
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        font.drawInBatch(text, x, y, color, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static ItemStack getItemStack(String itemId) {
        if (itemId == null || itemId.isBlank() || "minecraft:air".equals(itemId)) {
            return ItemStack.EMPTY;
        }

        ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
        if (resourceLocation == null) {
            return ItemStack.EMPTY;
        }

        Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
        return item == null ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    private static boolean containsSameItem(List<ItemStack> stacks, ItemStack target) {
        if (target.isEmpty()) {
            return false;
        }

        for (ItemStack stack : stacks) {
            if (ItemStack.isSameItem(stack, target)) {
                return true;
            }
        }

        return false;
    }
}
