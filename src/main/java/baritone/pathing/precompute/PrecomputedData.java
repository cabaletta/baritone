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

package baritone.pathing.precompute;

import baritone.Baritone;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BlockStateInterface;
import baritone.utils.accessor.IBlockStateFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PrecomputedData {

    private final int[] data = new int[Block.BLOCK_STATE_REGISTRY.size()];

    /**
     * bit layout (low to high)
     * <p>
     * 0 completed, 1 fullyPassable maybe, 2 fullyPassable, 3 canWalkThrough maybe, 4 canWalkThrough,
     * 5 canWalkOn maybe, 6 canWalkOn, 7 canPlaceAgainst, 8 never break this (the position independent half of avoidBreaking)
     */

    private static final int COMPLETED_MASK = 1;
    private static final int FULLY_PASSABLE_MAYBE_MASK = 1 << 1;
    private static final int FULLY_PASSABLE_MASK = 1 << 2;
    private static final int CAN_WALK_THROUGH_MAYBE_MASK = 1 << 3;
    private static final int CAN_WALK_THROUGH_MASK = 1 << 4;
    private static final int CAN_WALK_ON_MAYBE_MASK = 1 << 5;
    private static final int CAN_WALK_ON_MASK = 1 << 6;
    private static final int CAN_PLACE_AGAINST_MASK = 1 << 7;
    private static final int NEVER_BREAK_MASK = 1 << 8;

    // low FLAG_BITS of the word on a BlockState are the flags, the rest says which table wrote them
    private static final int FLAG_BITS = 10;
    private static final int FLAG_MASK = (1 << FLAG_BITS) - 1;
    private static final AtomicInteger GENERATION = new AtomicInteger();
    // is the mixin there? ask once. a failing instanceof on an interface that nobody implements
    // makes the jvm scan the whole supertype list every time and we do this a lot
    private static final boolean STAMPED_STATES = Blocks.AIR.defaultBlockState() instanceof IBlockStateFlags;
    // starts at 1 so a blank state (0) never looks like ours
    private final int generation = GENERATION.incrementAndGet();

    // if any of these change the table is garbage, so remember what it was built with
    private final boolean allowWalkOnMagmaBlocks;
    private final boolean allowVines;
    private final boolean assumeWalkOnLava;
    private final boolean allowWalkOnBottomSlab;
    private final List<Block> blocksToAvoid;
    private final List<Block> blocksToDisallowBreaking;

    private static volatile PrecomputedData shared;

    public PrecomputedData() {
        this.allowWalkOnMagmaBlocks = Baritone.settings().allowWalkOnMagmaBlocks.value;
        this.allowVines = Baritone.settings().allowVines.value;
        this.assumeWalkOnLava = Baritone.settings().assumeWalkOnLava.value;
        this.allowWalkOnBottomSlab = Baritone.settings().allowWalkOnBottomSlab.value;
        this.blocksToAvoid = new ArrayList<>(Baritone.settings().blocksToAvoid.value);
        this.blocksToDisallowBreaking = new ArrayList<>(Baritone.settings().blocksToDisallowBreaking.value);
    }

    // stone is still stone next search. we used to throw the whole table away every tick and
    // redo all the instanceof chains from scratch, plus allocate 25kb for the privilege
    public static PrecomputedData forCurrentSettings() {
        PrecomputedData s = shared;
        if (s == null || !s.matchesCurrentSettings()) {
            s = new PrecomputedData();
            shared = s;
        }
        return s;
    }

    private boolean matchesCurrentSettings() {
        return allowWalkOnMagmaBlocks == Baritone.settings().allowWalkOnMagmaBlocks.value
                && allowVines == Baritone.settings().allowVines.value
                && assumeWalkOnLava == Baritone.settings().assumeWalkOnLava.value
                && allowWalkOnBottomSlab == Baritone.settings().allowWalkOnBottomSlab.value
                && blocksToAvoid.equals(Baritone.settings().blocksToAvoid.value)
                && blocksToDisallowBreaking.equals(Baritone.settings().blocksToDisallowBreaking.value);
    }

    private int fillData(int id, BlockState state) {
        int blockData = 0;

        Ternary canWalkOnState = MovementHelper.canWalkOnBlockState(state);
        switch (canWalkOnState) {
            case YES -> blockData |= CAN_WALK_ON_MASK;
            case MAYBE -> blockData |= CAN_WALK_ON_MAYBE_MASK;
        }

        Ternary canWalkThroughState = MovementHelper.canWalkThroughBlockState(state);
        switch (canWalkThroughState) {
            case YES -> blockData |= CAN_WALK_THROUGH_MASK;
            case MAYBE -> blockData |= CAN_WALK_THROUGH_MAYBE_MASK;
        }

        Ternary fullyPassableState = MovementHelper.fullyPassableBlockState(state);
        switch (fullyPassableState) {
            case YES -> blockData |= FULLY_PASSABLE_MASK;
            case MAYBE -> blockData |= FULLY_PASSABLE_MAYBE_MASK;
        }

        // isBlockNormalCube goes through a guava cache that allocates on every single hit
        // 8% of the whole search was spent asking if air is a cube. it is not. get rekt guava
        Block block = state.getBlock();
        if (MovementHelper.isBlockNormalCube(state) || block == Blocks.GLASS || block instanceof StainedGlassBlock) {
            blockData |= CAN_PLACE_AGAINST_MASK;
        }

        if (blocksToDisallowBreaking.contains(block)
                || block == Blocks.ICE // ice becomes water, and water can mess up the path
                || block instanceof InfestedBlock) { // obvious reasons
            blockData |= NEVER_BREAK_MASK;
        }

        blockData |= COMPLETED_MASK;

        data[id] = blockData; // in theory, this is thread "safe" because every thread should compute the exact same int to write?
        return blockData;
    }

    // the flags live on the blockstate itself (mixin), stamped with our generation so an old table's answer gets ignored
    // the registry array is what fills that in, and is the whole story when there's no mixin (i.e. running the
    // pathfinder outside the game, which is a thing you can do now)
    private int flags(BlockState state) {
        if (STAMPED_STATES) {
            IBlockStateFlags stamped = (IBlockStateFlags) state;
            int word = stamped.baritone$getPathingFlags();
            if ((word >>> FLAG_BITS) == generation) {
                return word & FLAG_MASK;
            }
            int blockData = flagsFromRegistry(state);
            stamped.baritone$setPathingFlags((generation << FLAG_BITS) | blockData);
            return blockData;
        }
        return flagsFromRegistry(state);
    }

    private int flagsFromRegistry(BlockState state) {
        int id = Block.BLOCK_STATE_REGISTRY.getId(state);
        int blockData = data[id];
        if ((blockData & COMPLETED_MASK) == 0) { // we need to fill in the data
            blockData = fillData(id, state);
        }
        return blockData;
    }

    public boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        int blockData = flags(state);
        if ((blockData & CAN_WALK_ON_MAYBE_MASK) != 0) {
            return MovementHelper.canWalkOnPosition(bsi, x, y, z, state);
        } else {
            return (blockData & CAN_WALK_ON_MASK) != 0;
        }
    }

    public boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        int blockData = flags(state);
        if ((blockData & CAN_WALK_THROUGH_MAYBE_MASK) != 0) {
            return MovementHelper.canWalkThroughPosition(bsi, x, y, z, state);
        } else {
            return (blockData & CAN_WALK_THROUGH_MASK) != 0;
        }
    }

    public boolean fullyPassable(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        int blockData = flags(state);
        if ((blockData & FULLY_PASSABLE_MAYBE_MASK) != 0) {
            return MovementHelper.fullyPassablePosition(bsi, x, y, z, state);
        } else {
            return (blockData & FULLY_PASSABLE_MASK) != 0;
        }
    }

    // the block half of MovementHelper.canPlaceAgainst, the world border half is your problem
    public boolean canPlaceAgainst(BlockStateInterface bsi, BlockState state) {
        return (flags(state) & CAN_PLACE_AGAINST_MASK) != 0;
    }

    // the block half of MovementHelper.avoidBreaking, go look at the neighbours yourself
    public boolean neverBreak(BlockStateInterface bsi, BlockState state) {
        return (flags(state) & NEVER_BREAK_MASK) != 0;
    }
}
