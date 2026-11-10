package com.y271727uy.FRMC.mixin.kaleidoscopecookery;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.BambooTrayBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.BambooTrayRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.BambooTrayRecipe.Subtype;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.y271727uy.FRMC.integration.kaleidoscopecookery.KitchenRecipeIndex;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.Tags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BambooTrayBlockEntity.class)
public abstract class BambooTrayBlockEntityMixin {
    @Inject(method = "serverTick", remap = false, cancellable = true, require = 1, at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;above()Lnet/minecraft/core/BlockPos;"),
            @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;m_7494_()Lnet/minecraft/core/BlockPos;")})
    private static void frmc$skipEmptyTray(Level level, BlockPos pos, BlockState state, BambooTrayBlockEntity tray,
                                          CallbackInfo ci) {
        var items = tray.getItems();
        for (int slot = 0; slot < items.size(); slot++) {
            if (!items.get(slot).isEmpty()) {
                return;
            }
        }
        ci.cancel();
    }

    @Inject(method = "hasDryingExposure", remap = false, at = @At("HEAD"), cancellable = true, require = 1)
    private static void frmc$scanOnlyPossibleShade(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        int x = pos.getX();
        int z = pos.getZ();
        var chunk = server.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) {
            return;
        }
        if (level.canSeeSky(pos.above())) {
            cir.setReturnValue(true);
            return;
        }
        int localX = x & 15;
        int localZ = z & 15;
        int bottom = pos.getY() + 1;
        int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ);
        Block tray = ModBlocks.BAMBOO_TRAY.get();
        Predicate<BlockState> blocksDrying = state -> !state.isAir() && !state.is(tray)
                && !state.is(Tags.Blocks.GLASS_COLORLESS) && !state.is(Tags.Blocks.GLASS_PANES_COLORLESS);
        for (int sectionY = bottom >> 4; sectionY <= top >> 4; sectionY++) {
            var section = chunk.getSection(level.getSectionIndexFromSectionY(sectionY));
            if (section.hasOnlyAir() || !section.maybeHas(blocksDrying)) {
                continue;
            }
            for (int y = Math.max(bottom, sectionY << 4); y <= Math.min(top, (sectionY << 4) + 15); y++) {
                if (blocksDrying.test(section.getBlockState(localX, y & 15, localZ))) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
        cir.setReturnValue(true);
    }

    @WrapOperation(method = "serverTick", remap = false, require = 1, at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/RecipeManager;getAllRecipesFor(Lnet/minecraft/world/item/crafting/RecipeType;)Ljava/util/List;"),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/RecipeManager;m_44013_(Lnet/minecraft/world/item/crafting/RecipeType;)Ljava/util/List;")})
    private static List<BambooTrayRecipe> frmc$useOrderedSubtypeIndex(RecipeManager manager, RecipeType<?> type,
            Operation<List<BambooTrayRecipe>> original, @Local Subtype subtype) {
        return KitchenRecipeIndex.trayRecipes(manager, subtype);
    }

    @WrapOperation(method = "serverTick", remap = false, require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;"))
    private static Object frmc$recordResult(NonNullList<?> items, int slot, Object result, Operation<Object> original,
                                            @Share("resultChanged") LocalBooleanRef resultChanged) {
        resultChanged.set(true);
        return original.call(items, slot, result);
    }

    @WrapOperation(method = "serverTick", remap = false, require = 1,
            at = @At(value = "INVOKE", remap = false, target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/BambooTrayBlockEntity;refresh()V"))
    private static void frmc$syncOnlyResult(BambooTrayBlockEntity tray, Operation<Void> original,
            @Local(argsOnly = true) Level level, @Share("resultChanged") LocalBooleanRef resultChanged) {
        if (resultChanged.get()) {
            original.call(tray);
        } else {
            level.blockEntityChanged(tray.getBlockPos());
        }
    }
}
