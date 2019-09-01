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
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public final class BlockOptionalMeta {
    private final Block block;
    private final int meta;
    private final boolean noMeta;
    private final Set<IBlockState> blockstates;
    private final ImmutableSet<Integer> stateHashes;
    private final ImmutableSet<Integer> stackHashes;
    private static final Pattern pattern = Pattern.compile("^(.+?)(?::(\\d+))?$");
    private static final Map<Class<? extends Enum>, Enum<?>> normalizations;

    static {
        Map<Class<? extends Enum>, Enum<?>> _normalizations = new HashMap<>();
        Consumer<Enum> put = instance -> _normalizations.put(instance.getClass(), instance);
        put.accept(EnumFacing.NORTH);
        put.accept(EnumFacing.Axis.Y);
        put.accept(BlockStairs.EnumHalf.BOTTOM);
        put.accept(BlockStairs.EnumShape.STRAIGHT);
        put.accept(BlockLever.EnumOrientation.DOWN_X);
        put.accept(BlockDoublePlant.EnumBlockHalf.LOWER);
        put.accept(BlockSlab.EnumBlockHalf.BOTTOM);
        put.accept(BlockDoor.EnumDoorHalf.LOWER);
        normalizations = _normalizations;
    }

    private static <C extends Comparable<C>, P extends IProperty<C>> P castToIProperty(Object value) {
        //noinspection unchecked
        return (P) value;
    }

    @SuppressWarnings("unused")
    private static <C extends Comparable<C>, P extends IProperty<C>> C castToIPropertyValue(P iproperty, Object value) {
        //noinspection unchecked
        return (C) value;
    }

    public static IBlockState normalize(IBlockState state) {
        IBlockState newState = state;

        for (IProperty<?> property : state.getProperties().keySet()) {
            Class<?> valueClass = property.getValueClass();
            if (normalizations.containsKey(valueClass)) {
                try {
                    newState = newState.withProperty(
                        castToIProperty(property),
                        castToIPropertyValue(property, normalizations.get(valueClass))
                    );
                } catch (IllegalArgumentException ignored) {}
            }
        }

        return newState;
    }

    public static int stateMeta(IBlockState state) {
        return state.getBlock().getMetaFromState(normalize(state));
    }

    private static Set<IBlockState> getStates(@Nonnull Block block, @Nullable Integer meta) {
        return block.getBlockState().getValidStates().stream()
            .filter(blockstate -> meta == null || stateMeta(blockstate) == meta)
            .collect(Collectors.toCollection(HashSet::new));
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
        return ImmutableSet.copyOf(
            blockstates.stream()
                .map(state -> new ItemStack(
                    state.getBlock().getItemDropped(state, new Random(), 0),
                    state.getBlock().damageDropped(state)
                ))
                .map(stack -> ((IItemStack) (Object) stack).getBaritoneHash())
                .toArray(Integer[]::new)
        );
    }

    public BlockOptionalMeta(@Nonnull Block block, @Nullable Integer meta) {
        this.block = block;
        this.noMeta = isNull(meta);
        this.meta = noMeta ? 0 : meta;
        this.blockstates = getStates(block, meta);
        this.stateHashes = getStateHashes(blockstates);
        this.stackHashes = getStackHashes(blockstates);
    }

    public BlockOptionalMeta(@Nonnull Block block) {
        this(block, null);
    }

    public BlockOptionalMeta(@Nonnull String selector) {
        Matcher matcher = pattern.matcher(selector);

        if (!matcher.find()) {
            throw new IllegalArgumentException("invalid block selector");
        }

        MatchResult matchResult = matcher.toMatchResult();
        noMeta = matchResult.group(2) == null;

        ResourceLocation id = new ResourceLocation(matchResult.group(1));

        if (!Block.REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException("Invalid block ID");
        }

        block = Block.REGISTRY.getObject(id);
        meta = noMeta ? 0 : Integer.parseInt(matchResult.group(2));
        blockstates = getStates(block, getMeta());
        stateHashes = getStateHashes(blockstates);
        stackHashes = getStackHashes(blockstates);
    }

    public Block getBlock() {
        return block;
    }

    public Integer getMeta() {
        return noMeta ? null : meta;
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

        if (noMeta) {
            hash -= stack.getItemDamage();
        }

        return stackHashes.contains(hash);
    }

    @Override
    public String toString() {
        return String.format("BlockOptionalMeta{block=%s,meta=%s}", block, getMeta());
    }

    public static IBlockState blockStateFromStack(ItemStack stack) {
        //noinspection deprecation
        return Block.getBlockFromItem(stack.getItem()).getStateFromMeta(stack.getMetadata());
    }
}
