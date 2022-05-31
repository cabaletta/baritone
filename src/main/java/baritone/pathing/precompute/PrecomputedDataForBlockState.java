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

import baritone.utils.BlockStateInterface;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;

import java.lang.reflect.Array;
import java.util.Optional;
import java.util.function.Function;

public class PrecomputedDataForBlockState {
    boolean[] dataPerBlockState = new boolean[Block.BLOCK_STATE_IDS.size()]; // Has to be of type boolean due to otherwise it has a generic type
    boolean[] specialCases = new boolean[Block.BLOCK_STATE_IDS.size()]; // We can also be certain that size will return the highest as it fills in all positions with null until we get to the highest block state

    private final SpecialCaseFunction specialCaseHandler;
    private final Function<IBlockState, Optional<Boolean>> precomputer;

    public PrecomputedDataForBlockState(Function<IBlockState, Optional<Boolean>> precomputer, SpecialCaseFunction specialCaseHandler) {
        this.specialCaseHandler = specialCaseHandler;
        this.precomputer = precomputer;

        this.refresh();
    }

    public void refresh() {
        for (IBlockState state : Block.BLOCK_STATE_IDS) { // state should never be null
            Optional<Boolean> applied = precomputer.apply(state);

            int id = Block.BLOCK_STATE_IDS.get(state);

            if (applied.isPresent()) {
                dataPerBlockState[id] = applied.get();
                specialCases[id] = false;
            } else {
                dataPerBlockState[id] = false;
                specialCases[id] = true;
            }
        }
    }

    public boolean get(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        int id = Block.BLOCK_STATE_IDS.get(state);
        if (specialCases[id]) {
            return specialCaseHandler.apply(bsi, x, y, z, state);
        } else {
            return dataPerBlockState[id];
        }
    }

    interface SpecialCaseFunction {
        public boolean apply(BlockStateInterface bsi, int x, int y, int z, IBlockState blockState);
    }
}
