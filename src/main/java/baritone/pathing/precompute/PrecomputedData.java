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
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Optional;

import static baritone.pathing.movement.MovementHelper.isFlowing;
import static baritone.pathing.movement.MovementHelper.isWater;

public class PrecomputedData { // TODO add isFullyPassable
    private final int[] data = new int[Block.BLOCK_STATE_IDS.size()]; // Has to be of type boolean due to otherwise it has a generic type

    private final int completedMask = 0b1;
    private final int canWalkOnMask = 0b10;
    private final int canWalkOnSpecialMask = 0b100;
    private final int canWalkThroughMask = 0b1000;
    private final int canWalkThroughSpecialMask = 0b10000;

    public PrecomputedData() {
        Arrays.fill(data, 0);
    }

    private void fillData(int id, IBlockState state) {
        Optional<Boolean> canWalkOnState = MovementHelper.canWalkOnBlockState(state);
        if (canWalkOnState.isPresent()) {
            if (canWalkOnState.get()) {
                data[id] = data[id] | canWalkOnMask;
            }
        } else {
            data[id] = data[id] | canWalkOnSpecialMask;
        }

        Optional<Boolean> canWalkThroughState = MovementHelper.canWalkThroughBlockState(state);
        if (canWalkThroughState.isPresent()) {
            if (canWalkThroughState.get()) {
                data[id] = data[id] | canWalkThroughMask;
            }
        } else {
            data[id] = data[id] | canWalkThroughSpecialMask;
        }


        data[id] = data[id] | completedMask;
    }

    public boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        int id = Block.BLOCK_STATE_IDS.get(state);

        if ((data[id] & completedMask) == 0) { // we need to fill in the data
            fillData(id, state);
        }

        if ((data[id] & canWalkOnSpecialMask) != 0) {
            return MovementHelper.canWalkOnPosition(bsi, x, y, z, state);
        } else {
            return (data[id] & canWalkOnMask) != 0;
        }
    }

    public boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        int id = Block.BLOCK_STATE_IDS.get(state);

        if ((data[id] & completedMask) == 0) { // we need to fill in the data
            fillData(id, state);
        }

        if ((data[id] & canWalkThroughSpecialMask) != 0) {
            return MovementHelper.canWalkOnPosition(bsi, x, y, z, state);
        } else {
            return (data[id] & canWalkThroughMask) != 0;
        }
    }
}
