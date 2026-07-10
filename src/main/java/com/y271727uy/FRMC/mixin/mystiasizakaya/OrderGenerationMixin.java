package com.y271727uy.FRMC.mixin.mystiasizakaya;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.hiedacamellia.mystiasizakaya.content.orders.Addorder;
import org.hiedacamellia.mystiasizakaya.core.config.CommonConfig;
import org.hiedacamellia.mystiasizakaya.core.event.MIPlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.hiedacamellia.mystiasizakaya.core.event.MIPlayerEvent$EventBusVariableHandlers", remap = false)
public abstract class OrderGenerationMixin {
    @Unique
    private static final BlockPos FRMC$UNBOUND_TABLE = new BlockPos(-1, -1, -1);
    @Unique
    private static final int FRMC$FORCE_ORDER_AFTER_FAILED_ATTEMPTS = 12;
    @Unique
    private static final Map<UUID, Integer> FRMC$orderFailedAttempts = new HashMap<>();

    @Inject(
        method = "onPlayerTick",
        at = @At(
            value = "INVOKE",
            target = "Lorg/hiedacamellia/mystiasizakaya/core/event/MIPlayerEvent;getOnOpen(Lnet/minecraft/world/entity/player/Player;)Z"
        ),
        require = 1
    )
    private static void frmc$generateOrderWithGuarantee(TickEvent.PlayerTickEvent event, CallbackInfo ci) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        int interval = ((Integer) CommonConfig.ORDER_REFRESH_INTERVAL.get()).intValue();
        if (interval <= 0 || !MIPlayerEvent.getOnOpen(player) || player.level().getGameTime() % (long) interval != 0L) {
            return;
        }

        List<Integer> emptyTableIndexes = frmc$getEmptyTableIndexes(player);
        if (emptyTableIndexes.isEmpty()) {
            FRMC$orderFailedAttempts.remove(player.getUUID());
            return;
        }

        List<ItemStack> cuisineMenuPool = frmc$getMenuPool(MIPlayerEvent.getMenus(player));
        List<ItemStack> beverageMenuPool = frmc$getMenuPool(MIPlayerEvent.getMenusBeverages(player));
        if (cuisineMenuPool.isEmpty() || beverageMenuPool.isEmpty()) {
            FRMC$orderFailedAttempts.remove(player.getUUID());
            return;
        }

        UUID playerId = player.getUUID();
        int failedAttempts = FRMC$orderFailedAttempts.getOrDefault(playerId, 0);
        double probability = Math.max(0.0D, Math.min(1.0D, ((Double) CommonConfig.ORDER_REFRESH_PROBABILITY.get()).doubleValue()));
        boolean forceOrder = failedAttempts + 1 >= FRMC$FORCE_ORDER_AFTER_FAILED_ATTEMPTS;

        if (!forceOrder && ThreadLocalRandom.current().nextDouble() >= probability) {
            FRMC$orderFailedAttempts.put(playerId, failedAttempts + 1);
            return;
        }

        int orderIndex = emptyTableIndexes.get(ThreadLocalRandom.current().nextInt(emptyTableIndexes.size()));
        ItemStack cuisine = cuisineMenuPool.get(ThreadLocalRandom.current().nextInt(cuisineMenuPool.size()));
        ItemStack beverage = beverageMenuPool.get(ThreadLocalRandom.current().nextInt(beverageMenuPool.size()));
        Addorder.execute(beverage, cuisine, orderIndex, player);
        FRMC$orderFailedAttempts.remove(playerId);
    }

    @Redirect(
        method = "onPlayerTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/common/ForgeConfigSpec$DoubleValue;get()Ljava/lang/Object;"
        ),
        require = 1
    )
    private static Object frmc$disableOriginalOrderProbability(ForgeConfigSpec.DoubleValue instance) {
        if (instance == CommonConfig.ORDER_REFRESH_PROBABILITY) {
            return 0.0D;
        }
        return instance.get();
    }

    @Unique
    private static List<Integer> frmc$getEmptyTableIndexes(ServerPlayer player) {
        List<BlockPos> tables = MIPlayerEvent.getTables(player);
        List<String> cuisineOrders = MIPlayerEvent.getOrders(player);
        List<String> beverageOrders = MIPlayerEvent.getOrdersBeverages(player);
        List<Integer> emptyTableIndexes = new ArrayList<>();

        int size = Math.min(tables.size(), Math.min(cuisineOrders.size(), beverageOrders.size()));
        for (int i = 0; i < size; i++) {
            if (!FRMC$UNBOUND_TABLE.equals(tables.get(i))
                && "minecraft:air".equals(cuisineOrders.get(i))
                && "minecraft:air".equals(beverageOrders.get(i))) {
                emptyTableIndexes.add(i);
            }
        }

        return emptyTableIndexes;
    }

    @Unique
    private static List<ItemStack> frmc$getMenuPool(List<String> menuIds) {
        Set<ItemStack> menuPool = new LinkedHashSet<>();
        for (String menuId : menuIds) {
            if (menuId == null || menuId.isBlank() || "minecraft:air".equals(menuId)) {
                continue;
            }

            ResourceLocation itemId = ResourceLocation.tryParse(menuId);
            if (itemId == null) {
                continue;
            }

            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item != null) {
                ItemStack stack = item.getDefaultInstance();
                if (!stack.isEmpty()) {
                    menuPool.add(stack);
                }
            }
        }

        return new ArrayList<>(menuPool);
    }
}
