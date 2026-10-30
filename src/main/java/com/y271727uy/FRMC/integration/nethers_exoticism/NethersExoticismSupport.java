package com.y271727uy.FRMC.integration.nethers_exoticism;

import java.util.Map;
import net.mcreator.nethersexoticism.init.NethersExoticismModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

/** Shared lookups for Nether's Exoticism procedures that otherwise rebuild registries and block states on every tick. */
public final class NethersExoticismSupport {
    public static final ResourceLocation WOOL_PLACE = new ResourceLocation("minecraft", "block.wool.place");
    public static final ResourceLocation SWEET_BERRY_BREAK = new ResourceLocation("minecraft", "block.sweet_berry_bush.break");
    public static final ResourceLocation RAMBUTAN_ADVANCEMENT = new ResourceLocation("nethers_exoticism", "rambutan_adv");
    public static final ResourceLocation RAMBUTAN_HELMET_ADVANCEMENT = new ResourceLocation("nethers_exoticism", "rambutan_helmet_adv");
    public static final ResourceLocation RAMBUTAN_SHAKE_ADVANCEMENT = new ResourceLocation("nethers_exoticism", "rambutan_shake_adv");

    private static final Direction[] HORIZONTAL_THEN_VERTICAL = {
        Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN
    };
    private static final int[][] BOUDDHA_HAND_OFFSETS = {
        {0, 2, -1}, {0, 2, 1}, {1, 3, 0}, {2, 3, 0}, {-1, 4, 0}, {-2, 4, 0}
    };
    private static final Direction[] BOUDDHA_HAND_FACING = {
        Direction.NORTH, Direction.NORTH, Direction.WEST, Direction.WEST, Direction.WEST, Direction.WEST
    };
    private static final int[][] BOUDDHA_HAND_NETHERRACK = {
        {0, 5, 0}, {1, 5, 0}, {2, 5, 0}, {-1, 5, 0}, {-2, 5, 0},
        {0, 5, 1}, {0, 5, -1}, {0, 5, 2}, {0, 5, -2},
        {1, 5, 1}, {1, 5, 2}, {2, 5, 1},
        {-1, 5, -1}, {-1, 5, -2}, {-2, 5, -1},
        {1, 5, -1}, {1, 5, -2}, {2, 5, -1},
        {-1, 5, 1}, {-1, 5, 2}, {-2, 5, 1}
    };
    private static final int[][] JABOTICABA_EMPTY = {
        {0, 2, 0}, {-1, 2, 0}, {1, 2, 0}, {0, 3, 1}, {0, 3, -1}
    };
    private static final int[][] JABOTICABA_FRUIT = {
        {0, 2, 1}, {0, 2, -1}, {-1, 3, 0}, {1, 3, 0}
    };
    private static final int[][] JABOTICABA_LEAVES = {
        {0, 4, 0}, {0, 4, -1}, {0, 4, 1}, {-1, 4, 0}, {1, 4, 0},
        {1, 4, 1}, {-1, 4, 1}, {-1, 4, -1}, {1, 4, -1},
        {0, 5, 0}, {0, 5, -1}, {0, 5, 1}, {-1, 5, 0}, {1, 5, 0}
    };
    private static final int[][] KIWANO_LEAVES = {
        {0, 1, 0}, {1, 1, 0}, {0, 1, 1}, {0, 1, -1},
        {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
        {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}
    };
    private static final int[][] RAMBUTAN_LEAVES = {
        {0, 2, 1}, {0, 2, -1}, {1, 2, 0}, {-1, 2, 0}, {1, 2, 1}, {-1, 2, -1},
        {0, 3, 1}, {0, 3, -1}, {1, 3, 0}, {-1, 3, 0}, {1, 3, -1}, {-1, 3, 1},
        {1, 5, 0}, {2, 5, 0}, {-1, 5, 0}, {-2, 5, 0},
        {0, 5, 1}, {0, 5, 2}, {0, 5, -1}, {0, 5, -2},
        {-1, 5, -1}, {-1, 5, -2}, {-2, 5, -1},
        {1, 5, 1}, {1, 5, 2}, {2, 5, 1},
        {1, 5, -1}, {1, 5, -2}, {2, 5, -2}, {2, 5, -1},
        {-1, 5, 1}, {-1, 5, 2}, {-2, 5, 2}, {-2, 5, 1},
        {1, 6, 0}, {2, 6, 0}, {-1, 6, 0}, {-2, 6, 0},
        {0, 6, 1}, {0, 6, 2}, {0, 6, -1}, {0, 6, -2},
        {-1, 6, -1}, {-1, 6, -2}, {-2, 6, -1},
        {1, 6, 1}, {1, 6, 2}, {2, 6, 1},
        {1, 6, -1}, {1, 6, -2}, {2, 6, 2}, {2, 6, -1},
        {-1, 6, 1}, {-1, 6, 2}, {-2, 6, -2}, {-2, 6, 1},
        {0, 7, 0}, {1, 7, 0}, {-1, 7, 0}, {0, 7, 1}, {0, 7, -1}
    };
    private static final int[][] RAMBUTAN_FRUIT = {
        {-2, 4, 1}, {-1, 4, -2}, {2, 4, -1}, {1, 1, 1}
    };

    private NethersExoticismSupport() {
    }

    public static DamageSource cactus(LevelAccessor world) {
        return new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.CACTUS));
    }

    static BlockState kiwanoLeaves() {
        return NethersExoticismModBlocks.KIWANO_LEAVES.get().defaultBlockState();
    }

    static BlockState kiwanoFruit(Direction facing) {
        return withProperty(
            NethersExoticismModBlocks.KIWANO_LEAVES_STAGE_1.get().defaultBlockState(),
            "facing",
            facing
        );
    }

    static BlockState rambutanLeaves() {
        return NethersExoticismModBlocks.RAMBOUTAN_LEAVES.get().defaultBlockState();
    }

    public static BlockState rambutanFruit() {
        return NethersExoticismModBlocks.RAMBOUTAN_BLOCK.get().defaultBlockState();
    }

    static BlockState pitayaStem() {
        return NethersExoticismModBlocks.PITAYA_STEM.get().defaultBlockState();
    }

    public static BlockState pitayaFruit() {
        return NethersExoticismModBlocks.PITAYA_BLOCK.get().defaultBlockState();
    }

    public static BlockState buddhaHand() {
        return NethersExoticismModBlocks.BOUDDHA_S_HAND_BLOCK.get().defaultBlockState();
    }

    static BlockState boneColumn(Direction.Axis axis) {
        return withProperty(
            NethersExoticismModBlocks.BONE_COLUMN.get().defaultBlockState(),
            "axis",
            axis
        );
    }

    static BlockState jaboticabaEmpty(Direction.Axis axis) {
        return withProperty(
            NethersExoticismModBlocks.JABOTICABA_BRANCH_EMPTY.get().defaultBlockState(),
            "axis",
            axis
        );
    }

    static BlockState jaboticabaCrossing() {
        return NethersExoticismModBlocks.JABOTICABA_BRANCH_CROSSING.get().defaultBlockState();
    }

    static BlockState jaboticabaUp(Direction facing) {
        return withProperty(
            NethersExoticismModBlocks.JABOTICABA_BRANCH_UP.get().defaultBlockState(),
            "facing",
            facing
        );
    }

    static BlockState jaboticabaFruit() {
        return NethersExoticismModBlocks.JABOTICABA_BRANCH.get().defaultBlockState();
    }

    static BlockState jaboticabaLeaves() {
        return NethersExoticismModBlocks.JABOTICABA_LEAVES.get().defaultBlockState();
    }

    static void placeIfAir(LevelAccessor world, BlockPos origin, int dx, int dy, int dz, BlockState state) {
        BlockPos pos = origin.offset(dx, dy, dz);
        if (world.isEmptyBlock(pos)) {
            world.setBlock(pos, state, 3);
        }
    }

    public static void place(LevelAccessor world, BlockPos pos, BlockState state) {
        world.setBlock(pos, state, 3);
    }

    public static Direction.Axis axisOf(BlockState state) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty("axis");
        if (property instanceof EnumProperty<?> enumProperty && state.hasProperty(enumProperty)) {
            Object value = state.getValue(enumProperty);
            if (value instanceof Direction.Axis axis) {
                return axis;
            }
        }
        return Direction.Axis.Y;
    }

    public static Direction facingOf(BlockState state) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty("facing");
        if (property instanceof DirectionProperty directionProperty && state.hasProperty(directionProperty)) {
            return state.getValue(directionProperty);
        }
        return Direction.NORTH;
    }

    public static void growKiwanoFruit(LevelAccessor world, BlockPos origin) {
        for (Direction direction : HORIZONTAL_THEN_VERTICAL) {
            BlockPos target = origin.relative(direction);
            if (!world.isEmptyBlock(target)) {
                continue;
            }
            place(world, target, kiwanoFruit(direction));
            play(world, origin, WOOL_PLACE, SoundSource.NEUTRAL, 1.0F, 1.0F);
            return;
        }
    }

    public static void growBuddhaHandTree(LevelAccessor world, BlockPos origin) {
        place(world, origin, Blocks.BONE_BLOCK.defaultBlockState());
        for (int height = 1; height <= 4; height++) {
            placeIfAir(world, origin, 0, height, 0, Blocks.BONE_BLOCK.defaultBlockState());
        }
        for (int i = 0; i < BOUDDHA_HAND_OFFSETS.length; i++) {
            int[] offset = BOUDDHA_HAND_OFFSETS[i];
            placeIfAir(world, origin, offset[0], offset[1], offset[2], boneColumn(BOUDDHA_HAND_FACING[i].getAxis()));
        }
        for (int[] offset : BOUDDHA_HAND_NETHERRACK) {
            placeIfAir(world, origin, offset[0], offset[1], offset[2], Blocks.NETHERRACK.defaultBlockState());
        }
        placeIfAir(world, origin, -2, 3, 0, buddhaHand());
        placeIfAir(world, origin, 0, 1, -1, buddhaHand());
    }

    public static void growJaboticabaTree(LevelAccessor world, BlockPos origin) {
        Direction.Axis axis = axisOf(world.getBlockState(origin));
        place(world, origin, jaboticabaEmpty(axis));
        placeIfAir(world, origin, 0, 1, 0, jaboticabaCrossing());
        placeIfAir(world, origin, 0, 1, -1, jaboticabaUp(Direction.NORTH));
        placeIfAir(world, origin, 0, 1, 1, jaboticabaUp(Direction.SOUTH));
        placeIfAir(world, origin, 1, 1, 0, jaboticabaUp(Direction.EAST));
        placeIfAir(world, origin, -1, 1, 0, jaboticabaUp(Direction.WEST));
        for (int[] offset : JABOTICABA_EMPTY) {
            placeIfAir(world, origin, offset[0], offset[1], offset[2], jaboticabaEmpty(Direction.Axis.Y));
        }
        for (int[] offset : JABOTICABA_FRUIT) {
            placeIfAir(world, origin, offset[0], offset[1], offset[2], jaboticabaFruit());
        }
        for (int[] offset : JABOTICABA_LEAVES) {
            placeIfAir(world, origin, offset[0], offset[1], offset[2], jaboticabaLeaves());
        }
    }

    public static void growKiwanoTree(LevelAccessor world, BlockPos origin) {
        place(world, origin, Blocks.WARPED_HYPHAE.defaultBlockState());
        for (int[] offset : KIWANO_LEAVES) {
            placeIfAir(world, origin, offset[0], offset[1], offset[2], kiwanoLeaves());
        }
        placeIfAir(world, origin, -1, 0, -1, kiwanoFruit(Direction.WEST));
        placeIfAir(world, origin, -1, 1, 1, kiwanoFruit(Direction.UP));
        placeIfAir(world, origin, 1, 1, 1, kiwanoFruit(Direction.SOUTH));
    }

    public static void growPitaya(LevelAccessor world, BlockPos origin, boolean tall) {
        place(world, origin, pitayaStem());
        if (tall) {
            placeIfAir(world, origin, 0, 1, 0, pitayaStem());
            placeIfAir(world, origin, 0, 2, 0, pitayaFruit());
        } else {
            placeIfAir(world, origin, 0, 1, 0, pitayaFruit());
        }
    }

    public static void growRambutanTree(LevelAccessor world, BlockPos origin) {
        place(world, origin, NethersExoticismModBlocks.RAMBOUTAN_LOG.get().defaultBlockState());
        for (int height = 1; height <= 6; height++) {
            placeIfAir(world, origin, 0, height, 0, NethersExoticismModBlocks.RAMBOUTAN_LOG.get().defaultBlockState());
        }
        for (int[] offset : RAMBUTAN_LEAVES) {
            placeIfAir(world, origin, offset[0], offset[1], offset[2], rambutanLeaves());
        }
        for (int[] offset : RAMBUTAN_FRUIT) {
            placeIfAir(world, origin, offset[0], offset[1], offset[2], rambutanFruit());
        }
    }

    static void replaceNearbySpawners(LevelAccessor world, BlockPos origin, Block spawner, Block replacement, int negativeRadius, int positiveRadius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -negativeRadius; dx <= positiveRadius; dx++) {
            for (int dy = -negativeRadius; dy <= positiveRadius; dy++) {
                for (int dz = -negativeRadius; dz <= positiveRadius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    cursor.setWithOffset(origin, dx, dy, dz);
                    if (world.getBlockState(cursor).is(spawner)) {
                        world.setBlock(cursor, replacement.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static BlockState withProperty(BlockState state, String name, Comparable<?> value) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return property == null ? state : copyProperty(state, property, value);
    }

    public static BlockState copyProperties(BlockState from, BlockState to) {
        BlockState copied = to;
        for (Map.Entry<Property<?>, Comparable<?>> entry : from.getValues().entrySet()) {
            Property<?> property = copied.getBlock().getStateDefinition().getProperty(entry.getKey().getName());
            if (property == null) {
                continue;
            }
            copied = copyProperty(copied, property, entry.getValue());
        }
        return copied;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copyProperty(BlockState state, Property property, Comparable value) {
        try {
            if (property.getPossibleValues().contains(value)) {
                return state.setValue(property, value);
            }
        } catch (RuntimeException ignored) {
            // MCreator's generated copy also swallows incompatible property values.
        }
        return state;
    }

    public static void play(LevelAccessor world, BlockPos pos, ResourceLocation soundId, SoundSource source, float volume, float pitch) {
        if (!(world instanceof Level level)) {
            return;
        }
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
        if (sound == null) {
            return;
        }
        if (level.isClientSide()) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), sound, source, volume, pitch, false);
        } else {
            level.playSound(null, pos, sound, source, volume, pitch);
        }
    }
}
