package com.y271727uy.FRMC.mixin.manors_bounty;

import net.mcreator.manors_bounty.FruitLeavesBlock;
import net.mcreator.manors_bounty.FruitLeavesBlock.CanFruitProperty;
import net.mcreator.manors_bounty.network.ManorsBountyModVariables.MapVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

@Mixin(value = FruitLeavesBlock.class, remap = false)
public abstract class FruitLeavesBlockMixin extends LeavesBlock implements BonemealableBlock {
    @Shadow
    @Final
    public static IntegerProperty AGE_2;

    @Shadow
    @Final
    public static EnumProperty<CanFruitProperty> CAN_FRUIT;

    @Shadow
    @Final
    public static BooleanProperty RARE;

    @Unique
    private static final ResourceLocation FRMC$RARE_FRUIT_LEAVES_TAG = Objects.requireNonNull(ResourceLocation.tryParse("manors_bounty:rare_fruit_leaves"));
    @Unique
    private static final TagKey<Block> FRMC$RARE_FRUIT_LEAVES = BlockTags.create(FRMC$RARE_FRUIT_LEAVES_TAG);
    @Unique
    private static final ResourceLocation FRMC$BERRY_PICK_SOUND = Objects.requireNonNull(ResourceLocation.tryParse("minecraft:block.sweet_berry_bush.pick_berries"));
    @Unique
    private static final ResourceLocation FRMC$ITEM_PICKUP_SOUND = Objects.requireNonNull(ResourceLocation.tryParse("minecraft:entity.item.pickup"));
    @Unique
    private static final int FRMC$MAX_DROP_SCAN_DEPTH = 16;
    @Unique
    private static final int FRMC$AGE_UNRIPE = 1;
    @Unique
    private static final int FRMC$AGE_RIPE = 2;
    @Unique
    private static final double FRMC$NATURAL_GROWTH_SCALE = 0.4D;

    @Shadow
    protected abstract ItemStack getDropFruit();

    @Shadow
    protected abstract int getTickDropMin();

    @Shadow
    protected abstract int getTickDropMax();

    @Shadow
    protected abstract int getClickDropMin();

    @Shadow
    protected abstract int getClickDropMax();

    @Shadow
    protected abstract ItemStack getRareDropFruit();

    protected FruitLeavesBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * @author FRMC
     * @reason Avoid random ticks for stable non-fruiting leaves while preserving decay and active growth states.
     */
    @Overwrite(remap = false)
    public boolean isRandomlyTicking(BlockState state) {
        if (state.getValue(PERSISTENT)) {
            return false;
        }

        if (state.getValue(DISTANCE) == 7) {
            return true;
        }

        if (state.getValue(AGE_2) != 0) {
            return true;
        }

        return state.getValue(CAN_FRUIT) != CanFruitProperty.FALSE;
    }

    /**
     * @author FRMC
     * @reason Replace generated tick logic with a cleaner implementation while preserving shared fruit-leaf behavior.
     */
    @Overwrite(remap = false)
    public void randomTick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
        super.randomTick(blockstate, world, pos, random);

        BlockState currentState = world.getBlockState(pos);
        if (currentState.getBlock() != this || currentState.getValue(PERSISTENT)) {
            return;
        }

        if (!this.frmc$isExposed(world, pos)) {
            return;
        }

        CanFruitProperty canFruit = currentState.getValue(CAN_FRUIT);
        if (canFruit == CanFruitProperty.UNINITIALIZED) {
            this.frmc$initCanFruit(world, pos, currentState, random);
            return;
        }

        if (canFruit != CanFruitProperty.TRUE) {
            return;
        }

        int age = currentState.getValue(AGE_2);
        MapVariables variables = MapVariables.get(world);
        if (age == 0) {
            if (random.nextDouble() < this.frmc$getScaledGrowthChance(variables.fruit_tree_blossom)) {
                this.frmc$performGrow(world, pos, currentState, random);
            }
            return;
        }

        if (age == FRMC$AGE_UNRIPE) {
            if (random.nextDouble() < this.frmc$getScaledGrowthChance(variables.fruit_tree_fruit)) {
                this.frmc$performGrow(world, pos, currentState, random);
            }
            return;
        }

        if (age != FRMC$AGE_RIPE) {
            return;
        }

        double dropChance = variables.fruit_tree_drop;
        if (dropChance > 0.0D) {
            if (random.nextDouble() < dropChance) {
                this.frmc$dropFromTick(world, pos, currentState, random);
            }
        } else if (dropChance < 0.0D) {
            world.setBlock(pos, currentState.setValue(AGE_2, 0), 3);
        }
    }

    /**
     * @author FRMC
     * @reason Keep right-click harvesting behavior intact while reducing duplicated lookups.
     */
    @Overwrite(remap = false)
    public InteractionResult use(BlockState blockstate, Level world, BlockPos pos, Player entity, InteractionHand hand, BlockHitResult hit) {
        if (blockstate.getValue(AGE_2) != FRMC$AGE_RIPE) {
            return InteractionResult.PASS;
        }

        if (!world.isClientSide()) {
            world.setBlock(pos, blockstate.setValue(AGE_2, 0), 3);

            ItemStack drop = this.frmc$getDropForState(blockstate).copy();
            drop.setCount(this.frmc$getRandomCount(world.random, this.getClickDropMin(), this.getClickDropMax()));
            ItemHandlerHelper.giveItemToPlayer(entity, drop);

            this.frmc$playSound(world, pos, FRMC$BERRY_PICK_SOUND, SoundSource.BLOCKS);
            this.frmc$playSound(world, pos, FRMC$ITEM_PICKUP_SOUND, SoundSource.PLAYERS);
        }

        return InteractionResult.sidedSuccess(world.isClientSide());
    }

    /**
     * @author FRMC
     * @reason Keep bone meal targeting aligned with the rewritten shared growth flow.
     */
    @Overwrite(remap = false)
    public boolean isValidBonemealTarget(LevelReader worldIn, BlockPos pos, BlockState blockstate, boolean clientSide) {
        return blockstate.getValue(AGE_2) != FRMC$AGE_RIPE;
    }

    /**
     * @author FRMC
     * @reason Route bone meal growth through the rewritten shared growth path.
     */
    @Overwrite(remap = false)
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState blockstate) {
        this.frmc$performGrow(world, pos, blockstate, random);
    }

    @Unique
    private boolean frmc$isExposed(LevelAccessor world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            if (!world.getBlockState(adjacentPos).isFaceSturdy(world, adjacentPos, direction.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private ItemStack frmc$getDropForState(BlockState blockstate) {
        return blockstate.getValue(RARE) ? this.getRareDropFruit() : this.getDropFruit();
    }

    @Unique
    private void frmc$initCanFruit(LevelAccessor world, BlockPos pos, BlockState blockstate, RandomSource random) {
        double chance = MapVariables.get(world).fruit_tree_leaves_fruit_chance;
        CanFruitProperty value = random.nextDouble() < chance ? CanFruitProperty.TRUE : CanFruitProperty.FALSE;
        world.setBlock(pos, blockstate.setValue(CAN_FRUIT, value), 3);
    }

    @Unique
    private void frmc$performGrow(LevelAccessor world, BlockPos pos, BlockState blockstate, RandomSource random) {
        CanFruitProperty canFruit = blockstate.getValue(CAN_FRUIT);
        if (canFruit == CanFruitProperty.UNINITIALIZED) {
            this.frmc$initCanFruit(world, pos, blockstate, random);
            return;
        }

        int age = blockstate.getValue(AGE_2);
        if (!blockstate.getValue(PERSISTENT)) {
            if (canFruit != CanFruitProperty.TRUE) {
                return;
            }

            if (age == 0) {
                world.setBlock(pos, blockstate.setValue(AGE_2, FRMC$AGE_UNRIPE), 3);
                return;
            }

            if (age == FRMC$AGE_UNRIPE) {
                BlockState nextState = blockstate;
                if (blockstate.is(FRMC$RARE_FRUIT_LEAVES) && random.nextDouble() < MapVariables.get(world).rare_variant_chance) {
                    nextState = nextState.setValue(RARE, true);
                }

                world.setBlock(pos, nextState.setValue(AGE_2, FRMC$AGE_RIPE), 3);
            }
            return;
        }

        if (age == 0) {
            world.setBlock(pos, blockstate.setValue(AGE_2, FRMC$AGE_UNRIPE), 3);
        } else if (age == FRMC$AGE_UNRIPE) {
            world.setBlock(pos, blockstate.setValue(AGE_2, 0), 3);
        }
    }

    @Unique
    private void frmc$dropFromTick(ServerLevel world, BlockPos pos, BlockState blockstate, RandomSource random) {
        world.setBlock(pos, blockstate.setValue(AGE_2, 0), 3);

        ItemStack drop = this.frmc$getDropForState(blockstate).copy();
        drop.setCount(this.frmc$getRandomCount(random, this.getTickDropMin(), this.getTickDropMax()));

        double dropY = pos.getY();
        for (int depth = 1; depth <= FRMC$MAX_DROP_SCAN_DEPTH; depth++) {
            BlockState belowState = world.getBlockState(pos.below(depth));
            if (belowState.isAir()) {
                dropY = pos.getY() - depth;
                break;
            }

            if (!(belowState.getBlock() instanceof LeavesBlock)) {
                break;
            }
        }

        ItemEntity entityToSpawn = new ItemEntity(world, pos.getX() + 0.5D, dropY, pos.getZ() + 0.5D, drop);
        entityToSpawn.setPickUpDelay(10);
        world.addFreshEntity(entityToSpawn);
        this.frmc$playSound(world, pos, FRMC$BERRY_PICK_SOUND, SoundSource.BLOCKS);
    }

    @Unique
    private int frmc$getRandomCount(RandomSource random, int min, int max) {
        if (max <= min) {
            return min;
        }
        return Mth.nextInt(random, min, max);
    }

    @Unique
    private double frmc$getScaledGrowthChance(double originalChance) {
        return Math.max(0.0D, Math.min(1.0D, originalChance * FRMC$NATURAL_GROWTH_SCALE));
    }

    @Unique
    private void frmc$playSound(Level world, BlockPos pos, ResourceLocation soundId, SoundSource soundSource) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
        if (sound != null) {
            world.playSound(null, pos, sound, soundSource, 1.0F, 1.0F);
        }
    }
}



