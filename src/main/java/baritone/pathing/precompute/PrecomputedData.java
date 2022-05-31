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
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

import static baritone.pathing.movement.MovementHelper.isFlowing;
import static baritone.pathing.movement.MovementHelper.isWater;

public class PrecomputedData { // TODO add isFullyPassable

    PrecomputedDataForBlockState canWalkOn;
    PrecomputedDataForBlockState canWalkThrough;

    public PrecomputedData() { // currently designed for this to be remade on setting change, however that could always be changed
        long startTime = System.nanoTime();

        canWalkOn = new PrecomputedDataForBlockState((state) -> { // this is just copied from the old MovementHelperFunction
            Block block = state.getBlock();
            if (block == Blocks.AIR || block == Blocks.MAGMA) {
                return Optional.of(false);
            }
            if (state.isBlockNormalCube()) {
                return Optional.of(true);
            }
            if (block == Blocks.LADDER || (block == Blocks.VINE && Baritone.settings().allowVines.value)) { // TODO reconsider this
                return Optional.of(true);
            }
            if (block == Blocks.FARMLAND || block == Blocks.GRASS_PATH) {
                return Optional.of(true);
            }
            if (block == Blocks.ENDER_CHEST || block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
                return Optional.of(true);
            }
            if (isWater(block)) {
                return Optional.empty();
            }
            if (Baritone.settings().assumeWalkOnLava.value && MovementHelper.isLava(block)) {
                return Optional.empty();
            }

            if (block instanceof BlockSlab) {
                if (!Baritone.settings().allowWalkOnBottomSlab.value) {
                    if (((BlockSlab) block).isDouble()) {
                        return Optional.of(true);
                    }
                    return Optional.of(state.getValue(BlockSlab.HALF) != BlockSlab.EnumBlockHalf.BOTTOM);
                }
                return Optional.of(true);
            }

            return Optional.of(block instanceof BlockStairs);
        }, (bsi, x, y, z, blockState) -> { // This should just be water or lava, and could probably be made more efficient
            Block block = blockState.getBlock();
            if (isWater(block)) {
                // since this is called literally millions of times per second, the benefit of not allocating millions of useless "pos.up()"
                // BlockPos s that we'd just garbage collect immediately is actually noticeable. I don't even think it's a decrease in readability
                Block up = bsi.get0(x, y + 1, z).getBlock();
                if (up == Blocks.WATERLILY || up == Blocks.CARPET) {
                    return true;
                }
                if (MovementHelper.isFlowing(x, y, z, blockState, bsi) || block == Blocks.FLOWING_WATER) {
                    // the only scenario in which we can walk on flowing water is if it's under still water with jesus off
                    return isWater(up) && !Baritone.settings().assumeWalkOnWater.value;
                }
                // if assumeWalkOnWater is on, we can only walk on water if there isn't water above it
                // if assumeWalkOnWater is off, we can only walk on water if there is water above it
                return isWater(up) ^ Baritone.settings().assumeWalkOnWater.value;
            }

            if (Baritone.settings().assumeWalkOnLava.value && MovementHelper.isLava(block) && !MovementHelper.isFlowing(x, y, z, blockState, bsi)) {
                return true;
            }

            return false; // If we don't recognise it then we want to just return false to be safe.
        });

        canWalkThrough = new PrecomputedDataForBlockState((blockState) -> {
            Block block = blockState.getBlock();

            if (block == Blocks.AIR) {
                return Optional.of(true);
            }

            if (block == Blocks.FIRE || block == Blocks.TRIPWIRE || block == Blocks.WEB || block == Blocks.END_PORTAL || block == Blocks.COCOA || block instanceof BlockSkull || block instanceof BlockTrapDoor || block == Blocks.END_ROD) {
                return Optional.of(false);
            }

            if (Baritone.settings().blocksToAvoid.value.contains(block)) {
                return Optional.of(false);
            }

            if (block instanceof BlockDoor || block instanceof BlockFenceGate) {
                // Because there's no nice method in vanilla to check if a door is openable or not, we just have to assume
                // that anything that isn't an iron door isn't openable, ignoring that some doors introduced in mods can't
                // be opened by just interacting.
                return Optional.of(block != Blocks.IRON_DOOR);
            }

            if (block == Blocks.CARPET) {
                return Optional.empty();
            }

            if (block instanceof BlockSnow) {
                if (blockState.getValue(BlockSnow.LAYERS) >= 3) {
                    return Optional.of(false);
                }

                return Optional.empty();
            }

            if (block instanceof BlockLiquid) {
                if (blockState.getValue(BlockLiquid.LEVEL) != 0) {
                    return Optional.of(false);
                } else {
                    return Optional.empty();
                }
            }

            if (block instanceof BlockCauldron) {
                return Optional.of(false);
            }

            try { // A dodgy catch-all at the end, for most blocks with default behaviour this will work, however where blocks are special this will error out, and we can handle it when we have this information
                return Optional.of(block.isPassable(null, null));
            } catch (NullPointerException exception) {
                return Optional.empty();
            }
        }, (bsi, x, y, z, blockState) -> {
            Block block = blockState.getBlock();

            if (block == Blocks.CARPET) {
                return canWalkOn(bsi, x, y - 1, z);
            }

            if (block instanceof BlockSnow) { // TODO see if this case is necessary, shouldn't it also check this somewhere else?
                return canWalkOn(bsi, x, y - 1, z);
            }

            if (block instanceof BlockLiquid) {
                if (isFlowing(x, y, z, blockState, bsi)) {
                    return false;
                }
                // Everything after this point has to be a special case as it relies on the water not being flowing, which means a special case is needed.
                if (Baritone.settings().assumeWalkOnWater.value) {
                    return false;
                }

                IBlockState up = bsi.get0(x, y + 1, z);
                if (up.getBlock() instanceof BlockLiquid || up.getBlock() instanceof BlockLilyPad) {
                    return false;
                }
                return block == Blocks.WATER || block == Blocks.FLOWING_WATER;
            }

            return block.isPassable(bsi.access, bsi.isPassableBlockPos.setPos(x, y, z));
        });

        long endTime = System.nanoTime();

        System.out.println(endTime - startTime);
        Thread.dumpStack();
    }

    public boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        return canWalkOn.get(bsi, x, y, z, state);
    }

    public boolean canWalkOn(IPlayerContext ctx, BetterBlockPos pos, IBlockState state) {
        return canWalkOn(new BlockStateInterface(ctx), pos.x, pos.y, pos.z, state);
    }

    public boolean canWalkOn(IPlayerContext ctx, BlockPos pos) {
        return canWalkOn(new BlockStateInterface(ctx), pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean canWalkOn(IPlayerContext ctx, BetterBlockPos pos) {
        return canWalkOn(new BlockStateInterface(ctx), pos.x, pos.y, pos.z);
    }

    public boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z) {
        return canWalkOn(bsi, x, y, z, bsi.get0(x, y, z));
    }

    public boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        return false;
    }

    public boolean canWalkThrough(IPlayerContext ctx, BetterBlockPos pos) {
        return canWalkThrough(new BlockStateInterface(ctx), pos.x, pos.y, pos.z);
    }

    public boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z) {
        return canWalkThrough(bsi, x, y, z, bsi.get0(x, y, z));
    }

    /**
     *  Refresh the precomputed data, for use when settings have changed etc.
     */
    public void refresh() {
        long startTime = System.nanoTime();

        for (int i = 0; i < 10; i++) {
            canWalkThrough.refresh();
            canWalkOn.refresh();
        }

        long endTime = System.nanoTime();

        System.out.println(endTime - startTime);
    }
}
