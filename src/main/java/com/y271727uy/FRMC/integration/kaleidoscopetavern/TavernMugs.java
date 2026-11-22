package com.y271727uy.FRMC.integration.kaleidoscopetavern;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BottleBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.DrinkBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.DrinkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

public final class TavernMugs {
    public static final String EMPTY = "empty_wooden_mug";
    public static final String FILLED = "filled_wooden_mug";
    public static final ResourceLocation EMPTY_ID = new ResourceLocation("kaleidoscope_tavern", EMPTY);
    public static final ResourceLocation FILLED_ID = new ResourceLocation("kaleidoscope_tavern", FILLED);
    public static final ResourceLocation TANKARD_ID = new ResourceLocation("brewinandchewin", "tankard");
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 14, 8, 11);

    private TavernMugs() {
    }

    public static final class TankardItem extends BlockItem {
        public TankardItem(Block mug) {
            super(mug, new Item.Properties());
        }

        @Override
        public String getDescriptionId() {
            return "item.brewinandchewin.tankard";
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            var player = context.getPlayer();
            return player != null && player.isShiftKeyDown() ? super.useOn(context) : InteractionResult.PASS;
        }
    }

    public static Block emptyBlock() {
        return new BottleBlock(SHAPE) {
            @Override
            public Item asItem() {
                Item tankard = ForgeRegistries.ITEMS.getValue(TANKARD_ID);
                return tankard != null && tankard != Items.AIR ? tankard : super.asItem();
            }

            @Override
            public SoundType getSoundType(BlockState state) {
                return SoundType.WOOD;
            }
        };
    }

    public static final class FilledBlock extends DrinkBlock {
        public FilledBlock() {
            // DrinkBlock requires a count property with at least two values. The mug has no block item to stack.
            super(2, SHAPE, SHAPE);
        }

        @Override
        public SoundType getSoundType(BlockState state) {
            return SoundType.WOOD;
        }

        @Override
        public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
            if (level.getBlockEntity(pos) instanceof DrinkBlockEntity drink) {
                return drink.getItems().stream().filter(stack -> !stack.isEmpty())
                    .findFirst().map(ItemStack::copy).orElse(ItemStack.EMPTY);
            }
            return ItemStack.EMPTY;
        }
    }
}
