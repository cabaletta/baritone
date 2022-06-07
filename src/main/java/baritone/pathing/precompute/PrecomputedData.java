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

    public PrecomputedData() {
        canWalkOn = new PrecomputedDataForBlockState(MovementHelper::canWalkOnBlockState, MovementHelper::canWalkOnPosition);

        canWalkThrough = new PrecomputedDataForBlockState(MovementHelper::canWalkThroughBlockState, MovementHelper::canWalkThroughPosition);
    }

    public boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        return canWalkOn.get(bsi, x, y, z, state);
    }

    public boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        return canWalkThrough.get(bsi, x, y, z, state);
    }
}
