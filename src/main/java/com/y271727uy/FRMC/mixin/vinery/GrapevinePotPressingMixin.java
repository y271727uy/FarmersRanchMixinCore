package com.y271727uy.FRMC.mixin.vinery;

import com.y271727uy.FRMC.integration.vinery.GrapevinePressingBatch;
import com.y271727uy.FRMC.integration.vinery.GrapevinePressingRecipe;
import com.y271727uy.FRMC.integration.vinery.GrapevinePressingRegistry;
import com.y271727uy.FRMC.integration.vinery.GrapevinePotCrusherCache;
import com.y271727uy.FRMC.integration.vinery.TavernPressing;
import java.util.Comparator;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.satisfy.vinery.core.block.state.properties.GrapeProperty;
import net.satisfy.vinery.core.item.GrapeItem;
import net.satisfy.vinery.core.registry.GrapeTypeRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.satisfy.vinery.core.block.GrapevinePotBlock", remap = false)
public abstract class GrapevinePotPressingMixin implements EntityBlock {
    @Shadow @Final private static IntegerProperty STAGE;
    @Shadow @Final @Mutable private static IntegerProperty STORAGE;
    @Shadow @Final private static GrapeProperty GRAPEVINE_TYPE;

    @Unique
    private static final EnumProperty<TavernPressing> frmc$pressing =
        EnumProperty.create("frmc_pressing", TavernPressing.class);

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void frmc$extendStorage(CallbackInfo ci) {
        // Vinery still caps its native batch at six.
        STORAGE = IntegerProperty.create("storage", 0, GrapevinePressingRecipe.MAX_INPUT);
    }

    @Inject(method = {"createBlockStateDefinition", "m_7926_"}, at = @At("TAIL"), require = 1)
    private void frmc$addPressingState(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(frmc$pressing);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(frmc$pressing) == TavernPressing.DATA_PACK ? new GrapevinePressingBatch(pos, state) : null;
    }

    @Inject(method = {"fallOn", "m_142072_"}, at = @At("TAIL"), require = 1)
    private void frmc$completeBasinAdvancement(Level level, BlockState state, BlockPos pos, Entity entity,
                                               float fallDistance, CallbackInfo ci) {
        if (!(entity instanceof ServerPlayer player) || state.getValue(frmc$pressing) != TavernPressing.DATA_PACK
            || state.getValue(STAGE) != 5 || !(level.getBlockEntity(pos) instanceof GrapevinePressingBatch batch)) {
            return;
        }
        ResourceLocation outputId = ForgeRegistries.ITEMS.getKey(batch.result().getItem());
        if (outputId != null && outputId.getNamespace().equals("youkaishomecoming")
            && outputId.getPath().endsWith("_grape_juice")) {
            CriterionTrigger<?> criterion = CriteriaTriggers.getCriterion(new ResourceLocation("youkaishomecoming", "basin"));
            if (criterion instanceof PlayerTrigger trigger) {
                trigger.trigger(player);
            }
        }
    }

    @Inject(method = {"use", "m_6227_"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$pressJuice(BlockState state, Level level, BlockPos pos, Player player,
                                InteractionHand hand, BlockHitResult hit,
                                CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide) {
            return;
        }

        int stage = state.getValue(STAGE);
        ItemStack held = player.getItemInHand(hand);
        TavernPressing batch = state.getValue(frmc$pressing);
        if (batch == TavernPressing.DATA_PACK) {
            cir.setReturnValue(frmc$useDataPackBatch(state, level, pos, player, hand, held));
            return;
        }
        if (batch == TavernPressing.NONE) {
            if (stage != 0) {
                return;
            }
            InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack other = player.getItemInHand(otherHand);
            if (ModList.get().isLoaded("kaleidoscope_tavern")
                && (!(held.getItem() instanceof GrapeItem) || other.is(Items.BUCKET))) {
                batch = TavernPressing.forInput(held);
            }
            if (!held.isEmpty() && (batch == TavernPressing.NONE || !other.is(Items.BUCKET))
                && frmc$tryStartDataPackBatch(state, level, pos, player, held, other, batch != TavernPressing.NONE)) {
                cir.setReturnValue(InteractionResult.CONSUME);
                return;
            }
            if (batch == TavernPressing.NONE) {
                return;
            }
        }

        int stored = state.getValue(STORAGE);
        if (stage == 6 && stored == TavernPressing.BATCH_SIZE && held.is(Items.BUCKET)) {
            ItemStack output = batch.output();
            if (!output.isEmpty()) {
                level.setBlockAndUpdate(pos, state.getBlock().defaultBlockState());
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, output));
                GrapevinePotCrusherCache.clear(level, pos);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                cir.setReturnValue(InteractionResult.CONSUME);
                return;
            }
        } else if (stored < TavernPressing.BATCH_SIZE && TavernPressing.forInput(held) == batch) {
            int count = stored + 1;
            int nextStage = count == TavernPressing.BATCH_SIZE ? 3 : count < 3 ? 1 : 2;
            level.setBlockAndUpdate(pos, state.setValue(frmc$pressing, batch)
                .setValue(GRAPEVINE_TYPE, batch == TavernPressing.GREEN_GRAPE ? GrapeTypeRegistry.WHITE : GrapeTypeRegistry.RED)
                .setValue(STORAGE, count).setValue(STAGE, nextStage));
            frmc$consumeFruit(level, pos, player, held);
            cir.setReturnValue(InteractionResult.CONSUME);
            return;
        }
        cir.setReturnValue(InteractionResult.PASS);
    }

    @Unique
    private static boolean frmc$tryStartDataPackBatch(BlockState state, Level level, BlockPos pos, Player player,
                                                     ItemStack held, ItemStack other, boolean hasTavernRecipe) {
        GrapevinePressingRecipe recipe = level.getRecipeManager().getAllRecipesFor(GrapevinePressingRegistry.TYPE).stream()
            .filter(candidate -> candidate.ingredient().test(held))
            .filter(candidate -> !(hasTavernRecipe || held.getItem() instanceof GrapeItem) || frmc$matchesContainer(other, candidate))
            .min(Comparator.comparing((GrapevinePressingRecipe candidate) -> !frmc$matchesContainer(other, candidate))
                .thenComparingInt(GrapevinePressingRecipe::count)
                .thenComparing(GrapevinePressingRecipe::getId))
            .orElse(null);
        if (recipe == null || !level.setBlockAndUpdate(pos, state.setValue(frmc$pressing, TavernPressing.DATA_PACK)
            .setValue(GRAPEVINE_TYPE, recipe.white() ? GrapeTypeRegistry.WHITE : GrapeTypeRegistry.RED)
            .setValue(STORAGE, 1).setValue(STAGE, recipe.totalInputs() == 1 ? 3 : 1))) {
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof GrapevinePressingBatch batch)) {
            level.setBlockAndUpdate(pos, state);
            return false;
        }
        batch.start(recipe, held);
        frmc$consumeFruit(level, pos, player, held);
        return true;
    }

    @Unique
    private static boolean frmc$matchesContainer(ItemStack stack, GrapevinePressingRecipe recipe) {
        return recipe.container() == Items.AIR ? stack.isEmpty() : stack.is(recipe.container());
    }

    @Unique
    private static InteractionResult frmc$useDataPackBatch(BlockState state, Level level, BlockPos pos, Player player,
                                                           InteractionHand hand, ItemStack held) {
        if (!(level.getBlockEntity(pos) instanceof GrapevinePressingBatch batch)) {
            return InteractionResult.PASS;
        }
        int stored = state.getValue(STORAGE);
        boolean primary = batch.acceptsPrimary(held, stored);
        boolean additive = !primary && batch.acceptsAdditive(held);
        if (stored < batch.totalInputs() && (primary || additive)) {
            int count = stored + 1;
            int stage = count == batch.totalInputs() ? 3 : count < 3 ? 1 : 2;
            if (level.setBlockAndUpdate(pos, state.setValue(STORAGE, count).setValue(STAGE, stage))) {
                if (additive) {
                    batch.addAdditive();
                }
                frmc$consumeFruit(level, pos, player, held);
                return InteractionResult.CONSUME;
            }
        } else if (stored == batch.totalInputs() && state.getValue(STAGE) == 6 && batch.usesContainer(held)) {
            ItemStack output = batch.result();
            if (!output.isEmpty() && level.setBlockAndUpdate(pos, state.getBlock().defaultBlockState())) {
                level.removeBlockEntity(pos);
                player.setItemInHand(hand, held.isEmpty() ? output : ItemUtils.createFilledResult(held, player, output));
                GrapevinePotCrusherCache.clear(level, pos);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Unique
    private static void frmc$consumeFruit(Level level, BlockPos pos, Player player, ItemStack held) {
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
