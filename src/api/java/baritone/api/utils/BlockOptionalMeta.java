/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.api.utils;

import baritone.api.accessor.IItemStack;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IProperty;
import net.minecraft.state.properties.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class BlockOptionalMeta {

    private final Block block;
    private final Set<IBlockState> blockstates;
    private final ImmutableSet<Integer> stateHashes;
    private final ImmutableSet<Integer> stackHashes;
    private static final Pattern pattern = Pattern.compile("^(.+?)(?::(\\d+))?$");
    private static final Map<Object, Object> normalizations;

    public BlockOptionalMeta(@Nonnull Block block) {
        this.block = block;
        this.blockstates = getStates(block);
        this.stateHashes = getStateHashes(blockstates);
        this.stackHashes = getStackHashes(blockstates);
    }

    public BlockOptionalMeta(@Nonnull String selector) {
        Matcher matcher = pattern.matcher(selector);

        if (!matcher.find()) {
            throw new IllegalArgumentException("invalid block selector");
        }

        MatchResult matchResult = matcher.toMatchResult();

        ResourceLocation id = new ResourceLocation(matchResult.group(1));

        if (!IRegistry.BLOCK.containsKey(id)) {
            throw new IllegalArgumentException("Invalid block ID");
        }

        block = IRegistry.BLOCK.get(id);
        blockstates = getStates(block);
        stateHashes = getStateHashes(blockstates);
        stackHashes = getStackHashes(blockstates);
    }

    static {
        Map<Object, Object> _normalizations = new HashMap<>();
        Consumer<Enum> put = instance -> _normalizations.put(instance.getClass(), instance);
        put.accept(EnumFacing.NORTH);
        put.accept(EnumFacing.Axis.Y);
        put.accept(Half.BOTTOM);
        put.accept(StairsShape.STRAIGHT);
        put.accept(AttachFace.FLOOR);
        put.accept(DoubleBlockHalf.UPPER);
        put.accept(SlabType.BOTTOM);
        put.accept(DoorHingeSide.LEFT);
        put.accept(BedPart.HEAD);
        put.accept(RailShape.NORTH_SOUTH);
        _normalizations.put(BlockBanner.ROTATION, 0);
        _normalizations.put(BlockBed.OCCUPIED, false);
        _normalizations.put(BlockBrewingStand.HAS_BOTTLE[0], false);
        _normalizations.put(BlockBrewingStand.HAS_BOTTLE[1], false);
        _normalizations.put(BlockBrewingStand.HAS_BOTTLE[2], false);
        _normalizations.put(BlockButton.POWERED, false);
        // _normalizations.put(BlockCactus.AGE, 0);
        // _normalizations.put(BlockCauldron.LEVEL, 0);
        // _normalizations.put(BlockChorusFlower.AGE, 0);
        _normalizations.put(BlockChorusPlant.NORTH, false);
        _normalizations.put(BlockChorusPlant.EAST, false);
        _normalizations.put(BlockChorusPlant.SOUTH, false);
        _normalizations.put(BlockChorusPlant.WEST, false);
        _normalizations.put(BlockChorusPlant.UP, false);
        _normalizations.put(BlockChorusPlant.DOWN, false);
        // _normalizations.put(BlockCocoa.AGE, 0);
        // _normalizations.put(BlockCrops.AGE, 0);
        _normalizations.put(BlockDirtSnowy.SNOWY, false);
        _normalizations.put(BlockDoor.OPEN, false);
        _normalizations.put(BlockDoor.POWERED, false);
        // _normalizations.put(BlockFarmland.MOISTURE, 0);
        _normalizations.put(BlockFence.NORTH, false);
        _normalizations.put(BlockFence.EAST, false);
        _normalizations.put(BlockFence.WEST, false);
        _normalizations.put(BlockFence.SOUTH, false);
        // _normalizations.put(BlockFenceGate.POWERED, false);
        // _normalizations.put(BlockFenceGate.IN_WALL, false);
        _normalizations.put(BlockFire.AGE, 0);
        _normalizations.put(BlockFire.NORTH, false);
        _normalizations.put(BlockFire.EAST, false);
        _normalizations.put(BlockFire.SOUTH, false);
        _normalizations.put(BlockFire.WEST, false);
        _normalizations.put(BlockFire.UP, false);
        // _normalizations.put(BlockFrostedIce.AGE, 0);
        _normalizations.put(BlockGrass.SNOWY, false);
        // _normalizations.put(BlockHopper.ENABLED, true);
        // _normalizations.put(BlockLever.POWERED, false);
        // _normalizations.put(BlockLiquid.LEVEL, 0);
        // _normalizations.put(BlockMycelium.SNOWY, false);
        // _normalizations.put(BlockNetherWart.AGE, false);
        _normalizations.put(BlockLeaves.DISTANCE, false);
        // _normalizations.put(BlockLeaves.DECAYABLE, false);
        // _normalizations.put(BlockObserver.POWERED, false);
        _normalizations.put(BlockPane.NORTH, false);
        _normalizations.put(BlockPane.EAST, false);
        _normalizations.put(BlockPane.WEST, false);
        _normalizations.put(BlockPane.SOUTH, false);
        // _normalizations.put(BlockPistonBase.EXTENDED, false);
        // _normalizations.put(BlockPressurePlate.POWERED, false);
        // _normalizations.put(BlockPressurePlateWeighted.POWER, false);
        // _normalizations.put(BlockRailDetector.POWERED, false);
        // _normalizations.put(BlockRailPowered.POWERED, false);
        _normalizations.put(BlockRedstoneWire.NORTH, false);
        _normalizations.put(BlockRedstoneWire.EAST, false);
        _normalizations.put(BlockRedstoneWire.SOUTH, false);
        _normalizations.put(BlockRedstoneWire.WEST, false);
        // _normalizations.put(BlockReed.AGE, false);
        _normalizations.put(BlockSapling.STAGE, 0);
        _normalizations.put(BlockStandingSign.ROTATION, 0);
        _normalizations.put(BlockStem.AGE, 0);
        _normalizations.put(BlockTripWire.NORTH, false);
        _normalizations.put(BlockTripWire.EAST, false);
        _normalizations.put(BlockTripWire.WEST, false);
        _normalizations.put(BlockTripWire.SOUTH, false);
        _normalizations.put(BlockVine.NORTH, false);
        _normalizations.put(BlockVine.EAST, false);
        _normalizations.put(BlockVine.SOUTH, false);
        _normalizations.put(BlockVine.WEST, false);
        _normalizations.put(BlockVine.UP, false);
        _normalizations.put(BlockWall.UP, false);
        _normalizations.put(BlockWall.NORTH, false);
        _normalizations.put(BlockWall.EAST, false);
        _normalizations.put(BlockWall.WEST, false);
        _normalizations.put(BlockWall.SOUTH, false);
        normalizations = Collections.unmodifiableMap(_normalizations);
    }

    private static <C extends Comparable<C>, P extends IProperty<C>> P castToIProperty(Object value) {
        //noinspection unchecked
        return (P) value;
    }

    private static <C extends Comparable<C>, P extends IProperty<C>> C castToIPropertyValue(P iproperty, Object value) {
        //noinspection unchecked
        return (C) value;
    }

    public static IBlockState normalize(IBlockState state) {
        IBlockState newState = state;

        for (IProperty<?> property : state.getProperties()) {
            Class<?> valueClass = property.getValueClass();
            if (normalizations.containsKey(property)) {
                try {
                    newState = newState.with(
                            castToIProperty(property),
                            castToIPropertyValue(property, normalizations.get(property))
                    );
                } catch (IllegalArgumentException ignored) {}
            } else if (normalizations.containsKey(state.get(property))) {
                try {
                    newState = newState.with(
                            castToIProperty(property),
                            castToIPropertyValue(property, normalizations.get(state.get(property)))
                    );
                } catch (IllegalArgumentException ignored) {}
            } else if (normalizations.containsKey(valueClass)) {
                try {
                    newState = newState.with(
                            castToIProperty(property),
                            castToIPropertyValue(property, normalizations.get(valueClass))
                    );
                } catch (IllegalArgumentException ignored) {}
            }
        }

        return newState;
    }

    public static int stateMeta(IBlockState state) {
        return state.hashCode();
    }

    private static Set<IBlockState> getStates(@Nonnull Block block) {
        return new HashSet<>(block.getStateContainer().getValidStates());
    }

    private static ImmutableSet<Integer> getStateHashes(Set<IBlockState> blockstates) {
        return ImmutableSet.copyOf(
                blockstates.stream()
                        .map(IBlockState::hashCode)
                        .toArray(Integer[]::new)
        );
    }

    private static ImmutableSet<Integer> getStackHashes(Set<IBlockState> blockstates) {
        //noinspection ConstantConditions
        /*return ImmutableSet.copyOf(
                blockstates.stream()
                        .map(state -> new ItemStack(
                                state.getBlock().getItemDropped(state, ctx.world(), null, 0),
                                state.getBlock().(state)
                        ))
                        .map(stack -> ((IItemStack) (Object) stack).getBaritoneHash())
                        .toArray(Integer[]::new)
        );*/
        return ImmutableSet.of();
    }

    public Block getBlock() {
        return block;
    }

    public boolean matches(@Nonnull Block block) {
        return block == this.block;
    }

    public boolean matches(@Nonnull IBlockState blockstate) {
        Block block = blockstate.getBlock();
        return block == this.block && stateHashes.contains(blockstate.hashCode());
    }

    public boolean matches(ItemStack stack) {
        //noinspection ConstantConditions
        int hash = ((IItemStack) (Object) stack).getBaritoneHash();

            hash -= stack.getDamage();

        return stackHashes.contains(hash);
    }

    @Override
    public String toString() {
        return String.format("BlockOptionalMeta{block=%s}", block);
    }

    public IBlockState getAnyBlockState() {
        if (blockstates.size() > 0) {
            return blockstates.iterator().next();
        }

        return null;
    }
}
