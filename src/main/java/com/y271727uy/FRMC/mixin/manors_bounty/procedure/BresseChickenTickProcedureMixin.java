package com.y271727uy.FRMC.mixin.manors_bounty.procedure;

import net.mcreator.manors_bounty.entity.BresseChickenEntity;
import net.mcreator.manors_bounty.init.ManorsBountyModBlocks;
import net.mcreator.manors_bounty.init.ManorsBountyModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

@Mixin(targets = "net.mcreator.manors_bounty.procedures.BresseChickenTickProcedure")
public abstract class BresseChickenTickProcedureMixin {
    @Unique
    private static final int FRMC$MAX_EGG_LAY_TIME = 9000;
    @Unique
    private static final String FRMC$CHARLIE_NAME = "CHARLIE";
    @Unique
    private static final String FRMC$BLUE_TEXTURE = "bresse_chicken_blue";
    @Unique
    private static final String FRMC$PINK_TEXTURE = "bresse_chicken_pink";
    @Unique
    private static final ResourceLocation FRMC$EGG_SOUND_ID = Objects.requireNonNull(ResourceLocation.tryParse("minecraft:entity.chicken.egg"));

    /**
     * @author FRMC
     * @reason Rewrite the generated chicken tick procedure to preserve behavior while reducing per-tick allocations and redundant work.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (!(entity instanceof BresseChickenEntity chicken) || chicken.isBaby()) {
            return;
        }

        frmc$updateTexture(chicken);

        int eggTime = chicken.getEntityData().get(BresseChickenEntity.DATA_EggLayTime);
        if (eggTime > 0) {
            chicken.getEntityData().set(BresseChickenEntity.DATA_EggLayTime, eggTime - 1);
            return;
        }

        ItemStack eggStack = frmc$getEggStack(chicken);
        BlockPos belowPos = BlockPos.containing(x, y - 0.1D, z);
        if (world.getBlockState(belowPos).is(ManorsBountyModBlocks.HAY_NEST.get()) && frmc$tryPlaceEggInNest(world, belowPos, eggStack)) {
            chicken.getEntityData().set(BresseChickenEntity.DATA_EggLayTime, FRMC$MAX_EGG_LAY_TIME);
            return;
        }

        if (!world.isClientSide() && world instanceof Level level) {
            ItemEntity itemEntity = new ItemEntity(level, x, y, z, eggStack.copy());
            itemEntity.setPickUpDelay(10);
            level.addFreshEntity(itemEntity);
            frmc$playLaySound(level, chicken.blockPosition());
        }

        chicken.getEntityData().set(BresseChickenEntity.DATA_EggLayTime, FRMC$MAX_EGG_LAY_TIME);
    }

    @Unique
    private static void frmc$updateTexture(BresseChickenEntity chicken) {
        if (frmc$isCharlie(chicken)) {
            chicken.getEntityData().set(BresseChickenEntity.DATA_new_texture, FRMC$BLUE_TEXTURE);
            chicken.setTexture(FRMC$BLUE_TEXTURE);
            return;
        }

        chicken.setTexture(chicken.getEntityData().get(BresseChickenEntity.DATA_origin_texture));
    }

    @Unique
    private static boolean frmc$isCharlie(BresseChickenEntity chicken) {
        return chicken.hasCustomName() && FRMC$CHARLIE_NAME.equalsIgnoreCase(chicken.getName().getString());
    }

    @Unique
    private static ItemStack frmc$getEggStack(BresseChickenEntity chicken) {
        String originTexture = chicken.getEntityData().get(BresseChickenEntity.DATA_origin_texture);
        if (FRMC$PINK_TEXTURE.equals(originTexture)) {
            return new ItemStack(ManorsBountyModItems.PINK_BRESSE_CHICKEN_EGG.get());
        }
        return new ItemStack(ManorsBountyModItems.BRESSE_CHICKEN_EGG.get());
    }

    @Unique
    private static boolean frmc$tryPlaceEggInNest(LevelAccessor world, BlockPos nestPos, ItemStack eggStack) {
        BlockEntity blockEntity = world.getBlockEntity(nestPos);
        if (blockEntity == null) {
            return false;
        }

        boolean placed = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                .map(capability -> {
                    if (!(capability instanceof IItemHandlerModifiable handler)) {
                        return false;
                    }

                    int slotCount = Math.min(3, handler.getSlots());
                    for (int slot = 0; slot < slotCount; slot++) {
                        if (handler.getStackInSlot(slot).isEmpty()) {
                            handler.setStackInSlot(slot, eggStack.copy());
                            blockEntity.setChanged();
                            frmc$playLaySound(world, nestPos);
                            return true;
                        }
                    }

                    return false;
                })
                .orElse(false);

        return placed;
    }

    @Unique
    private static void frmc$playLaySound(LevelAccessor world, BlockPos pos) {
        if (!world.isClientSide() && world instanceof Level level) {
            frmc$playLaySound(level, pos);
        }
    }

    @Unique
    private static void frmc$playLaySound(Level level, BlockPos pos) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(FRMC$EGG_SOUND_ID);
        if (sound != null) {
            level.playSound(null, pos, sound, SoundSource.NEUTRAL, 0.5F, 0.8F + level.getRandom().nextFloat() * 0.4F);
        }
    }
}
