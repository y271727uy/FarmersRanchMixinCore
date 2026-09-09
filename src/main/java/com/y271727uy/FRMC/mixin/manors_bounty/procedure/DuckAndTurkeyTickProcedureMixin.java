package com.y271727uy.FRMC.mixin.manors_bounty.procedure;

import net.mcreator.manors_bounty.entity.MallardDuckEntity;
import net.mcreator.manors_bounty.entity.TurkeyEntity;
import net.mcreator.manors_bounty.init.ManorsBountyModBlocks;
import net.mcreator.manors_bounty.init.ManorsBountyModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = {
        "net.mcreator.manors_bounty.procedures.MallardDuckTickProcedure",
        "net.mcreator.manors_bounty.procedures.TurkeyTickProcedure"
})
public abstract class DuckAndTurkeyTickProcedureMixin {
    @Unique
    private static final int FRMC$MAX_EGG_LAY_TIME = 9000;
    @Unique
    private static final int FRMC$TIMER_STEP = 20;

    /**
     * @author FRMC
     * @reason Avoid dirtying and synchronizing the egg timer every entity tick.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (!(entity instanceof Mob bird) || bird.isBaby()) {
            return;
        }

        EntityDataAccessor<Integer> timerAccessor;
        Item eggItem;
        if (bird instanceof MallardDuckEntity) {
            timerAccessor = MallardDuckEntity.DATA_EggLayTime;
            eggItem = ManorsBountyModItems.MALLARD_DUCK_EGG.get();
        } else if (bird instanceof TurkeyEntity) {
            timerAccessor = TurkeyEntity.DATA_EggLayTime;
            eggItem = ManorsBountyModItems.TURKEY_EGG.get();
        } else {
            return;
        }

        int eggTime = bird.getEntityData().get(timerAccessor);
        if (eggTime > 0) {
            if (bird.tickCount % FRMC$TIMER_STEP != 0 && eggTime > FRMC$TIMER_STEP) {
                return;
            }
            int remaining = Math.max(0, eggTime - Math.min(FRMC$TIMER_STEP, eggTime));
            bird.getEntityData().set(timerAccessor, remaining);
            if (remaining > 0) {
                return;
            }
        }

        ItemStack eggStack = new ItemStack(eggItem);
        BlockPos belowPos = BlockPos.containing(x, y - 0.1D, z);
        if (world.getBlockState(belowPos).is(ManorsBountyModBlocks.HAY_NEST.get()) && frmc$tryPlaceEggInNest(world, belowPos, eggStack)) {
            bird.getEntityData().set(timerAccessor, FRMC$MAX_EGG_LAY_TIME);
            return;
        }

        if (!world.isClientSide() && world instanceof Level level) {
            ItemEntity itemEntity = new ItemEntity(level, x, y, z, eggStack);
            itemEntity.setPickUpDelay(10);
            level.addFreshEntity(itemEntity);
            frmc$playLaySound(level, bird.blockPosition());
        }

        bird.getEntityData().set(timerAccessor, FRMC$MAX_EGG_LAY_TIME);
    }

    @Unique
    private static boolean frmc$tryPlaceEggInNest(LevelAccessor world, BlockPos nestPos, ItemStack eggStack) {
        BlockEntity blockEntity = world.getBlockEntity(nestPos);
        if (blockEntity == null) {
            return false;
        }

        return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).map(capability -> {
            if (!(capability instanceof IItemHandlerModifiable handler)) {
                return false;
            }
            int slotCount = Math.min(3, handler.getSlots());
            for (int slot = 0; slot < slotCount; slot++) {
                if (handler.getStackInSlot(slot).isEmpty()) {
                    handler.setStackInSlot(slot, eggStack.copy());
                    blockEntity.setChanged();
                    if (!world.isClientSide() && world instanceof Level level) {
                        frmc$playLaySound(level, nestPos);
                    }
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }

    @Unique
    private static void frmc$playLaySound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 0.5F, 0.8F + level.getRandom().nextFloat() * 0.4F);
    }
}
