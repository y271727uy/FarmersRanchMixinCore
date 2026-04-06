package com.y271727uy.FRMC.mixin.manors_bounty;

import net.mcreator.manors_bounty.FruitLeavesBlock.CanFruitProperty;
import net.mcreator.manors_bounty.HangingFruitLeavesBlock;
import net.mcreator.manors_bounty.network.ManorsBountyModVariables.MapVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = HangingFruitLeavesBlock.class, remap = false)
public abstract class HangingFruitLeavesBlockMixin extends LeavesBlock implements BonemealableBlock {
    @Shadow
    @Final
    public static IntegerProperty AGE_7;

    @Shadow
    @Final
    public static EnumProperty<CanFruitProperty> CAN_FRUIT;

    @Unique
    private static final int FRMC$AGE_FLOWER = 1;
    @Unique
    private static final int FRMC$AGE_SMALL_FRUIT = 3;
    @Unique
    private static final int FRMC$AGE_GROWING_FRUIT = 5;
    @Unique
    private static final int FRMC$AGE_RIPE = 7;
    @Unique
    private static final double FRMC$NATURAL_GROWTH_SCALE = 0.4D;

    @Shadow
    protected abstract BlockState getFruitBlock();

    @Shadow
    protected abstract ItemStack getRareDropItem();

    protected HangingFruitLeavesBlockMixin(BlockBehaviour.Properties properties) {
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

        if (state.getValue(AGE_7) != 0) {
            return true;
        }

        return state.getValue(CAN_FRUIT) != CanFruitProperty.FALSE;
    }

    /**
     * @author FRMC
     * @reason Replace the generated neighbor update logic with a cleaner equivalent implementation.
     */
    @Overwrite(remap = false)
    public void neighborChanged(BlockState blockstate, Level world, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean moving) {
        super.neighborChanged(blockstate, world, pos, neighborBlock, fromPos, moving);

        int age = blockstate.getValue(AGE_7);
        if (age <= FRMC$AGE_FLOWER) {
            return;
        }

        BlockPos belowPos = pos.below();
        if (world.getBlockState(belowPos).getBlock() != this.getFruitBlock().getBlock()) {
            world.setBlock(pos, blockstate.setValue(AGE_7, FRMC$AGE_FLOWER), 3);
        }
    }

    /**
     * @author FRMC
     * @reason Replace the generated random tick logic with a compatibility-safe implementation that preserves behavior.
     */
    @Overwrite(remap = false)
    public void randomTick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
        super.randomTick(blockstate, world, pos, random);

        BlockState currentState = world.getBlockState(pos);
        if (currentState.getBlock() != this || currentState.getValue(PERSISTENT)) {
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

        BlockPos belowPos = pos.below();
        BlockState belowState = world.getBlockState(belowPos);
        if (!this.frmc$canUseFruitSpace(belowState)) {
            return;
        }

        int age = currentState.getValue(AGE_7);
        MapVariables variables = MapVariables.get(world);
        if (age == 0) {
            if (random.nextDouble() <= this.frmc$getScaledGrowthChance(variables.fruit_tree_blossom)) {
                this.frmc$performGrow(world, pos, currentState, random);
            }
            return;
        }

        if (age <= FRMC$AGE_GROWING_FRUIT) {
            if (random.nextDouble() <= this.frmc$getScaledGrowthChance(variables.fruit_tree_fruit)) {
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
                this.frmc$dropFruit(world, pos, belowPos, currentState);
            }
        } else if (dropChance < 0.0D) {
            this.frmc$clearFruit(world, pos, belowPos, currentState);
        }
    }

    /**
     * @author FRMC
     * @reason Keep bonemeal target checks aligned with the rewritten growth logic.
     */
    @Overwrite(remap = false)
    public boolean isValidBonemealTarget(LevelReader worldIn, BlockPos pos, BlockState blockstate, boolean clientSide) {
        if (!(worldIn instanceof LevelAccessor world)) {
            return false;
        }

        if (blockstate.getValue(AGE_7) >= FRMC$AGE_RIPE) {
            return false;
        }

        return this.frmc$canUseFruitSpace(world.getBlockState(pos.below()));
    }

    /**
     * @author FRMC
     * @reason Route bonemeal growth through the rewritten shared growth path.
     */
    @Overwrite(remap = false)
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState blockstate) {
        this.frmc$performGrow(world, pos, blockstate, random);
    }

    @Unique
    private boolean frmc$canUseFruitSpace(BlockState belowState) {
        return belowState.canBeReplaced() || belowState.getBlock() == this.getFruitBlock().getBlock();
    }

    @Unique
    private void frmc$initCanFruit(LevelAccessor world, BlockPos pos, BlockState blockstate, RandomSource random) {
        double chance = MapVariables.get(world).fruit_tree_leaves_fruit_chance;
        CanFruitProperty value = random.nextDouble() < chance ? CanFruitProperty.TRUE : CanFruitProperty.FALSE;
        world.setBlock(pos, blockstate.setValue(CAN_FRUIT, value), 3);
    }

    @Unique
    private void frmc$performGrow(LevelAccessor world, BlockPos pos, BlockState blockstate, RandomSource random) {
        if (blockstate.getValue(CAN_FRUIT) == CanFruitProperty.UNINITIALIZED) {
            this.frmc$initCanFruit(world, pos, blockstate, random);
            return;
        }

        int age = blockstate.getValue(AGE_7);
        if (!blockstate.getValue(PERSISTENT)) {
            if (age == 0) {
                world.setBlock(pos, blockstate.setValue(AGE_7, FRMC$AGE_FLOWER), 3);
                return;
            }

            if (age == FRMC$AGE_FLOWER) {
                world.setBlock(pos, blockstate.setValue(AGE_7, FRMC$AGE_SMALL_FRUIT), 3);
                world.setBlock(pos.below(), this.getFruitBlock(), 3);
                return;
            }

            if (age == FRMC$AGE_SMALL_FRUIT) {
                world.setBlock(pos, blockstate.setValue(AGE_7, FRMC$AGE_GROWING_FRUIT), 3);
                this.frmc$setFruitAge(world, pos.below(), 1);
                return;
            }

            if (age == FRMC$AGE_GROWING_FRUIT) {
                world.setBlock(pos, blockstate.setValue(AGE_7, FRMC$AGE_RIPE), 3);
                this.frmc$setFruitAge(world, pos.below(), 2);
            }
            return;
        }

        if (age == 0) {
            world.setBlock(pos, blockstate.setValue(AGE_7, FRMC$AGE_FLOWER), 3);
        } else if (age == FRMC$AGE_FLOWER) {
            world.setBlock(pos, blockstate.setValue(AGE_7, 0), 3);
        }
    }

    @Unique
    private void frmc$setFruitAge(LevelAccessor world, BlockPos fruitPos, int age) {
        BlockState fruitState = world.getBlockState(fruitPos);
        if (fruitState.getBlock() == this.getFruitBlock().getBlock()) {
            world.setBlock(fruitPos, fruitState.setValue(BlockStateProperties.AGE_2, age), 3);
        }
    }

    @Unique
    private void frmc$dropFruit(ServerLevel world, BlockPos pos, BlockPos belowPos, BlockState blockstate) {
        this.frmc$clearFruit(world, pos, belowPos, blockstate);

        ItemEntity entityToSpawn = new ItemEntity(
                world,
                pos.getX(),
                pos.getY() - 1.0D,
                pos.getZ(),
                this.getRareDropItem().copy()
        );
        entityToSpawn.setPickUpDelay(10);
        world.addFreshEntity(entityToSpawn);
    }

    @Unique
    private void frmc$clearFruit(LevelAccessor world, BlockPos pos, BlockPos belowPos, BlockState blockstate) {
        world.setBlock(pos, blockstate.setValue(AGE_7, FRMC$AGE_FLOWER), 3);
        world.setBlock(belowPos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Unique
    private double frmc$getScaledGrowthChance(double originalChance) {
        return Math.max(0.0D, Math.min(1.0D, originalChance * FRMC$NATURAL_GROWTH_SCALE));
    }
}
