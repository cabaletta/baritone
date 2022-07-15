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

import baritone.pathing.movement.MovementHelper;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import java.util.Optional;

public class PrecomputedData { // TODO add isFullyPassable

    private final int[] data = new int[Block.BLOCK_STATE_IDS.size()];

    private final int completedMask = 0b1;
    private final int canWalkOnMask = 0b10;
    private final int canWalkOnSpecialMask = 0b100;
    private final int canWalkThroughMask = 0b1000;
    private final int canWalkThroughSpecialMask = 0b10000;

    private int fillData(int id, IBlockState state) {
        int blockData = 0;

        Optional<Boolean> canWalkOnState = MovementHelper.canWalkOnBlockState(state);
        if (canWalkOnState.isPresent()) {
            if (canWalkOnState.get()) {
                blockData |= canWalkOnMask;
            }
        } else {
            blockData |= canWalkOnSpecialMask;
        }

        Optional<Boolean> canWalkThroughState = MovementHelper.canWalkThroughBlockState(state);
        if (canWalkThroughState.isPresent()) {
            if (canWalkThroughState.get()) {
                blockData |= canWalkThroughMask;
            }
        } else {
            blockData |= canWalkThroughSpecialMask;
        }

        blockData |= completedMask;

        data[id] = blockData; // in theory, this is thread "safe" because every thread should compute the exact same int to write?
        return blockData;
    }

    public boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        int id = Block.BLOCK_STATE_IDS.get(state);
        int blockData = data[id];

        if ((blockData & completedMask) == 0) { // we need to fill in the data
            blockData = fillData(id, state);
        }

        if ((blockData & canWalkOnSpecialMask) != 0) {
            return MovementHelper.canWalkOnPosition(bsi, x, y, z, state);
        } else {
            return (blockData & canWalkOnMask) != 0;
        }
    }

    public boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        int id = Block.BLOCK_STATE_IDS.get(state);
        int blockData = data[id];

        if ((blockData & completedMask) == 0) { // we need to fill in the data
            blockData = fillData(id, state);
        }

        if ((blockData & canWalkThroughSpecialMask) != 0) {
            return MovementHelper.canWalkThroughPosition(bsi, x, y, z, state);
        } else {
            return (blockData & canWalkThroughMask) != 0;
        }
    }
}
